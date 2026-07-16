# Kubernetes Helm Event Driven Microservices

Three services on Kubernetes, split across namespaces: 
`data` for the stateful backends (Postgres, RabbitMQ), 
`dev` and `prod` for the app.
Each service owns its Helm chart and deploys independently — same chart for both environments, with a values overlay carrying the differences. 
ExternalName Services sit between the app and its infrastructure, so moving a database to RDS is a one-line change the app never sees. 
Prod is a real boundary: restricted Pod Security, default-deny networking, resource quotas. 
Argo CD reconciles everything from Git, so the cluster is whatever the repo says — and shipping to prod means committing a version, not running a deploy.

## Prerequisites
- JDK 21
- Node 24
- Docker
- DockerHub
- Github
- git
- kubectl
- helm
- minikube
- minikube addons:
    - `minikube addons enable ingress`: nginx ingress controller for the web UI
    - `minikube addons enable metrics-server`: for kubectl top + HPA later
- need the following if running locally:
  - maven
  - postgres
  - rabbitmq
  - argo cd cli

---

## Project Structure

```ignorelang
k8s-helm-event-driven-microservices/
├── docker-compose.yml          # local dev: all services + rabbit + postgres
├── README.md
├── .github/workflows/          # all CI lives at the repo ROOT 
│   ├── order-service.yml        # path-filtered to order-service/**
│   ├── notification-service.yml # path-filtered to notification-service/**
│   └── ui.yml                  # path-filtered to ui/**
├── order-service/              # standalone microservice 
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd, .mvn/
│   ├── Dockerfile               # slim: copies pre-built target/app.jar
│   ├── src/main/java/com/k8sdemo/order/...
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   ├── application-local.yaml
│   │   ├── application-dev.yaml
│   │   ├── application-prod.yaml
│   │   └── db/changelog/        # Liquibase changelogs
│   └── helm/                    # its OWN chart
├── notification-service/        # standalone microservice 
│   └── (same shape as order-service)
├── ui/                         # standalone microservice 
│   ├── package.json
│   ├── vite.config.ts
│   ├── src/
│   ├── Dockerfile               # multi-stage: node build -> nginx
│   └── helm/
├── infra/                      # cluster setup: raw manifests you kubectl apply
│   └── k8s/
│       ├── namespaces.yaml      # dev / prod / data namespaces + labels
│       ├── data/                # backing services (stands in for RDS + Amazon MQ)
│       │   ├── orders-db.yaml         # Postgres for order-service
│       │   ├── notifications-db.yaml  # Postgres for notification-service
│       │   └── rabbitmq.yaml          # official image + STOMP plugins
│       ├── dev/                 # dev-only cluster config
│       │   └── externalnames.yaml     # ExternalName Services -> data tier
│       └── prod/                # prod-only cluster config
│           ├── externalnames.yaml
│           ├── network-policy.yaml    # restrict cross-namespace traffic
│           └── resource-quota.yaml    # cap prod namespace resources
└── argocd/                      # Argo Applications (one per service × env)
    ├── order-service/
    │   ├── dev.yaml
    │   └── prod.yaml
    ├── notification-service/
    │   ├── dev.yaml
    │   └── prod.yaml
    └── ui/
        ├── dev.yaml
        └── prod.yaml
```

---

## Architecture

- Five moving parts, two clean boundaries. Read this diagram before writing a line of code — every later stage is just making one piece of it real.
```
   ┌───────────────┐
   │      ui       │
   │ React + nginx │
   └───────┬───────┘
           │ REST  (nginx proxies /api)
           ▼
┌──────────┼───────────────────────── minikube cluster ─────────────────────────────────┐
│          │                                                                            │
│  ┌───────▼───────────┐                                ┌────────────────────────┐      │
│  │   order-service   │                                │  notification-service  │      │
│  │    Spring Boot    │                                │      Spring Boot       │      │
│  └───┬───────────┬───┘                                └────┬──────────────┬────┘      │
│      │           │                                         │              │           │
│      │           │        ┌────────────────────┐           │              │           │
│      │           └───────▶│      RabbitMQ      │◀──────────┘              │           │
│      │                    │ topic exchange+DLQ │                          │           │
│      │                    └────────────────────┘                          │           │
│      │                                                                    │           │
│      ▼                                                                    ▼           │
│  ┌───────────────────┐                                ┌────────────────────────┐      │
│  │     orders-db     │                                │    notifications-db    │      │
│  │  Postgres (data)  │                                │    Postgres (data)     │      │
│  └───────────────────┘                                └────────────────────────┘      │
│                                                                                       │
└───────────────────────────────────────────────────────────────────────────────────────┘

  PENDING ──▶ OrderCreated ──▶ SENT ──▶ NotificationSent ──▶ NOTIFIED
```

**order-service** — Spring Boot REST API. Accepts orders, persists to its own
Postgres, publishes `OrderCreated` to RabbitMQ, listens for `NotificationSent`.

**notification-service** — Spring Boot consumer. Listens for `OrderCreated`,
records a notification in its *own* Postgres, publishes `NotificationSent`.

**ui** — React/TypeScript (Vite) served by nginx, which proxies `/api` to
order-service by Kubernetes DNS name.

### The saga
- This is the sequence every later stage exists to support. Four events, two services, zero direct calls between them.

```ignorelang
order-service          RabbitMQ          notification-service
       │                     │                       │
  POST /orders               │                       │
       │  save PENDING       │                       │
       ├────────────────────▶│  OrderCreated         │
       │                     ├──────────────────────▶│
       │                     │                  save SENT
       │                     │◀──────────────────────┤
       │  NotificationSent   │                       │
       │◀────────────────────┤                       │
  save NOTIFIED              │                       │
       │                     │                       │
```

An order starts `PENDING`. `OrderCreated` goes on the broker.
notification-service picks it up, does its work, emits `NotificationSent`.
order-service hears that and flips the order to `NOTIFIED`.

No orchestrator. No service calling another service. Each one reacts to events
and owns its own decision — that's a *choreographed* saga.

Two properties fall out of it:

- **Database-per-service.** Two Postgres instances, no shared tables. If they
  shared a database they'd be one deployable pretending to be two.
- **No direct calls.** order-service doesn't know notification-service exists.
  Kill the consumer, place an order — the order still succeeds, and the
  notification happens whenever it comes back. The broker is all they share.

That's what makes them independently deployable, and why each owns its own
Helm chart.

---

## Run Application

### Infrastructure Setup
- Create Namespaces (dev/prod/data)
  - `k apply -f infra/k8s/namespaces.yaml`
- Create Data Tier (data)
  - `k apply -f infra/k8s/data/orders-db.yaml`
  - `k apply -f infra/k8s/data/notifications-db.yaml`
  - `k apply -f infra/k8s/data/rabbitmq.yaml`
- Create ExternalNames (dev/prod → data)
  - `k apply -f infra/k8s/dev/externalnames.yaml`
  - `k apply -f infra/k8s/prod/externalnames.yaml`
- Create Prod-only policies
  - `k apply -f infra/k8s/prod/network-policy.yaml`
  - `k apply -f infra/k8s/prod/resource-quota.yaml`
- Set up Argo
  - see Argo's section
    - if already set up `k apply -R -f argocd/` 
      - deploys all six apps - dev and prod (three services each)

#### Apply To Specific Environment
- DEV
  - `k apply -f argocd/order-service/dev.yaml -f argocd/notification-service/dev.yaml -f argocd/ui/dev.yaml`
- PROD
  - `k apply -f argocd/order-service/prod.yaml -f argocd/notification-service/prod.yaml -f argocd/ui/prod.yaml`

### Start Locally
- start apps
  - `docker compose` or `locally`

### Local Service Ports
- ui
  - `localhost:3000`
- order-service
  - `localhost:8080`
- notification-service
  - `localhost:8081`
- postgres databases:
  - `notifications`
  - `orders`
- rabbitMQ dashboard:
  - http://localhost:15672/#/
  - guest/guest

### Port-Forward
- ui
  - `k -n dev port-forward svc/ui 8081:8080`
- order-service
  - `k -n dev port-forward svc/order-service 8080:8080`
- notification-service
  - `k -n dev port-forward svc/notification-service 8082:8080`
- argo ui
  - `k port-forward -n argocd svc/argocd-server 8090:443`
- grafana
  - `k -n monitoring port-forward svc/monitoring-grafana 3001:80`

---

## Docker
- point Docker at minikube's daemon so images are visible to the cluster (Section 10):
  - `eval $(minikube docker-env)`
- Build Docker Files
  - `docker build -t order-service:0.1.0 ./order-service`
  - `docker build -t notification-service:0.1.0 ./notification-service`
  - `docker build -t ui:0.1.0 ./ui`

---

## Docker Compose
- build both service JARs (runs tests too):
  - `mvn -f order-service/pom.xml clean package`
  - `mvn -f notification-service/pom.xml clean package`
- start up:
  - `docker compose up --build`

---

## Minikube
- start
  - `minikube start --cpus=4 --memory=7000 --cni=calico --kubernetes-version=stable`
    - `--cni=calico`: minikube's default CNI ignores NetworkPolicies — without this,
        `prod/network-policy.yaml` applies cleanly and enforces nothing

---

## Helm
- install into the dev namespace:
  - helm install order-service ./order-service/helm -n dev

---

## Argo

### Setup
- create namespace
  - `k create namespace argocd`
- install Argo CD
  - `k apply --server-side -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml`
    - `--server-side`: without it, the ApplicationSet CRD exceeds kubectl's 256KB annotation limit and silently fails to install
  - wait for pods:
  - `k -n argocd get pods -w`
- forward-port
  - `kubectl port-forward -n argocd svc/argocd-server 8090:443`
- UI: https://localhost:8090  
  - user: admin
  - password:
    - `k -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d; echo`

### Deployment
- commit the argocd manifests
- apply
  - `k apply -R -f argocd/`
  - `-R`: recurses into the per-service subdirectories
- verify
  - `k -n argocd get applications`

---

## Clean Up

### Delete All Namespaces
- `k delete namespace dev prod data argocd`

### Delete Persistent Volume
- delete all
- `kubectl delete pv --all`
- Delete by `name`
  - Get PVs: `kubectl get pv`
  - `kubectl delete pv <name1> <name2>`

### Delete Argo Cascades
- Deletes namespace
  - order-service, notification-service, ui (pods, Services, Secrets)
```bash
k delete -R -f argocd/ --ignore-not-found

k delete namespace dev prod data

k delete namespace argocd
```

### Delete Argo For Specific Environment
```bash
# delete Dev only
kubectl delete -f argocd/order-service/dev.yaml -f argocd/notification-service/dev.yaml -f argocd/ui/dev.yaml

# delete Prod only
kubectl delete -f argocd/order-service/prod.yaml -f argocd/notification-service/prod.yaml -f argocd/ui/prod.yaml

```
