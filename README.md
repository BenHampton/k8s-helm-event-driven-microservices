# Kubernetes Helm Event Driven Microservices

Three services on Kubernetes, split across namespaces: 
`data` for the stateful backends (Postgres, RabbitMQ), 
`dev` and `prod` for the app.
Each service owns its Helm chart and deploys independently — same chart for both environments, with a values overlay carrying the differences. 
ExternalName Services sit between the app and its infrastructure, so moving a database to RDS is a one-line change the app never sees. 
Prod is a real boundary: restricted Pod Security, default-deny networking, resource quotas. 
Argo CD reconciles everything from Git, so the cluster is whatever the repo says — and shipping to prod means committing a version, not running a deploy.

# Prerequisites
- JDK 21
- Node 24
- Docker
- DockerHub
- Github
- git
- kubectl
- helm
- minikube
- need the following if running locally:
  - maven
  - postgres
  - rabbitmq
  - argo cd cli

---

# Project Structure

```ignorelang
k8s-helm-event-driven-microservices/
├── docker-compose.yml          # local dev: all services + rabbit + postgres
├── README.md
├── .github/workflows/           # ── all CI lives at the repo ROOT ──
│   ├── order-service.yml        # path-filtered to order-service/**
│   ├── notification-service.yml # path-filtered to notification-service/**
│   └── ui.yml                   # path-filtered to ui/**
├── order-service/               # ── standalone microservice ──
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd, .mvn/
│   ├── Dockerfile               
│   ├── src/main/java/com/k8sdemo/order/...
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   ├── application-local.yaml
│   │   ├── application-dev.yaml
│   │   ├── application-prod.yaml
│   │   └── db/changelog/        # Liquibase changelogs
│   └── helm/                    # its OWN chart
├── notification-service/        # ── standalone microservice ──
│   └── (same shape as order-service)
├── ui/                          # ── standalone microservice ──
│   ├── package.json
│   ├── vite.config.ts
│   ├── src/
│   ├── Dockerfile               # multi-stage: node build -> nginx
│   └── helm/
├── infra/                       # ── cluster setup: raw manifests you kubectl apply ──
│   └── k8s/
│       ├── namespaces.yaml      # dev / prod / data namespaces + labels
│       ├── data/                # backing services (stands in for RDS + Amazon MQ)
│       │   ├── orders-db/              # one Postgres per service PER ENV —
│       │   │   ├── dev.yaml            # dev and prod never share a database
│       │   │   └── prod.yaml
│       │   ├── notifications-db/
│       │   │   ├── dev.yaml
│       │   │   └── prod.yaml
│       │   └── rabbitmq/               # official image + STOMP plugins
│       │       ├── dev.yaml
│       │       └── prod.yaml
│       ├── dev/                  # dev-only cluster config
│       │   └── externalnames.yaml      # ExternalName Services -> data tier
│       └── prod/                 # prod-only cluster config
│           ├── externalnames.yaml
│           ├── network-policy.yaml     # restrict cross-namespace traffic
│           └── resource-quota.yaml     # cap prod namespace resources
└── argocd/                       # Argo Applications (one per service × env)
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

# Architecture

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
  
  orders-db / notifications-db / rabbitmq are ExternalName aliases. Each
  namespace resolves them to its own backend: dev → *-dev, prod → *-prod.
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

- **Database-per-service.** Each service owns its own Postgres — and each
  environment owns its own pair, so `data` holds four. No shared tables
  anywhere. If two services shared a database they'd be one deployable
  pretending to be two; if dev and prod shared one, a test fixture would be
  production data.
- **No direct calls.** order-service doesn't know notification-service exists.
  Kill the consumer, place an order — the order still succeeds, and the
  notification happens whenever it comes back. The broker is all they share.

That's what makes them independently deployable, and why each owns its own
Helm chart.

---

# Run Application

## Minikube
- start
  - `minikube start --cpus=4 --memory=7000 --cni=calico --kubernetes-version=stable`
    - `--cni=calico`: minikube's default CNI ignores NetworkPolicies — without this,
      `prod/network-policy.yaml` applies cleanly and enforces nothing
- addons
  - `minikube addons enable ingress`: nginx ingress controller for the web UI
  - `minikube addons enable metrics-server`: for kubectl top + HPA later

## Minikube Infrastructure Setup
- Create Namespaces (dev/prod/data)
  - `k apply -f infra/k8s/namespaces.yaml`
  - verify: `k get namespace`
- Create Data Tier (data)
  - `k apply -R -f infra/k8s/data/`
  - verify: `k -n data get pods,svc,pvc`
- Create ExternalNames (dev/prod → data)
  - `k apply -f infra/k8s/dev/externalnames.yaml`
  - `k apply -f infra/k8s/prod/externalnames.yaml`
  - verify: `kubectl -n <namespace> get svc`
- (Prod Only) - Create Prod-only policies
  - `k apply -f infra/k8s/prod/network-policy.yaml`
  - `k apply -f infra/k8s/prod/resource-quota.yaml`
- Create Services In Argo
***IMPORTANT*** see Argo's section if you have not set it up yet
  - `k apply -R -f argocd/` 
  - verify: `kubectl -n argocd get applications`
  - deploys all six apps - dev and prod (three services each)
- Verify Everything is Configured
  - `k -n <namespace> get pods,svc,pvc`

## Start Locally
- start apps
  - `docker compose` or `locally`

#### Local Service Ports
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

#### Port-Forward
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

#### Apply To Specific Environment
- DEV
  - Data Tiers
    - `k apply -f infra/k8s/data/orders-db/dev.yaml`
    - `k apply -f infra/k8s/data/notifications-db/dev.yaml`
    - `k apply -f infra/k8s/data/rabbitmq/dev.yaml`
  - ExternalNames
    - `k apply -f infra/k8s/dev/externalnames.yaml`
  - Argo
    - `k apply -f argocd/order-service/dev.yaml -f argocd/notification-service/dev.yaml -f argocd/ui/dev.yaml`
- PROD
  - Data Tiers
    - `k apply -f infra/k8s/data/orders-db/prod.yaml`
    - `k apply -f infra/k8s/data/notifications-db/prod.yaml`
    - `k apply -f infra/k8s/data/rabbitmq/prod.yaml`
  - ExternalNames
    - `k apply -f infra/k8s/prod/externalnames.yaml`
  - Argo
    - `k apply -f argocd/order-service/prod.yaml -f argocd/notification-service/prod.yaml -f argocd/ui/prod.yaml`

---

# Docker
- point Docker at minikube's daemon so images are visible to the cluster (Section 10):
  - `eval $(minikube docker-env)`
- Build Docker Files
  - `docker build -t order-service:0.1.0 ./order-service`
  - `docker build -t notification-service:0.1.0 ./notification-service`
  - `docker build -t ui:0.1.0 ./ui`

---

# Docker Compose
- build both service JARs (ui's builds itself):
  - `mvn -f order-service/pom.xml clean package`
  - `mvn -f notification-service/pom.xml clean package`
- start up:
  - `docker compose up --build`

---

# Helm

## Install to dev
- `helm install order-service ./order-service/helm -n dev`
- `helm install notification-service ./notification-service/helm -n dev`
- `helm install ui ./ui/helm -n dev`
- verify: `k -n dev get pods`

## Install to prod
Same charts, `values-prod.yaml` layered on top. The prod overlays pin image
SHAs — override with `--set image.tag=latest` if those aren't in your registry.
- `helm install order-service ./order-service/helm -n prod -f ./order-service/helm/values-prod.yaml`
- `helm install notification-service ./notification-service/helm -n prod -f ./notification-service/helm/values-prod.yaml`
- `helm install ui ./ui/helm -n prod -f ./ui/helm/values-prod.yaml`

## Inspect before installing
- render without applying: `helm template order-service ./order-service/helm`
- with the prod overlay: `helm template order-service ./order-service/helm -f ./order-service/helm/values-prod.yaml`
- dry run against the cluster: `helm install order-service ./order-service/helm -n dev --dry-run`
- lint: `helm lint ./order-service/helm`

## Upgrade
- `helm upgrade order-service ./order-service/helm -n dev`
- create-or-update in one command: `helm upgrade --install order-service ./order-service/helm -n dev`
- wait for healthy, roll back on failure: `helm upgrade --install order-service ./order-service/helm -n dev --wait --atomic`

## Inspect what's installed
- `helm list -n dev`
- `helm get values order-service -n dev`
- `helm get manifest order-service -n dev`
- `helm history order-service -n dev`

## Roll back
- `helm rollback order-service -n dev`          # previous revision
- `helm rollback order-service 3 -n dev`        # a specific one

## Uninstall
- `helm uninstall order-service notification-service ui -n dev`

---

# Argo

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

# Clean Up Cluster

### Delete All (dev/prod)
```bash

# Delete the Argo Applications - Argo cascades, removes the Pods, Services, and Secrets it deployed
k delete -R -f argocd/                    

# Delete backends - they live in the `data` namespace
k delete -R -f infra/k8s/data/           

# Delete Persistent Volume - PVCs outlive their StatefulSet by design, data is supposed to survive the workloads
k -n data delete pvc --all                

# Delete namespaces
k delete namespace dev prod               

# Argo stays up. To remove it too: 
k delete namespace argocd

```

### Verify Delete
```bash
# no dev, no prod (data remains unless you delete it)
k get ns                

# only kube-system and ingress-nginx                  
k get pods -A                       

# empty      
k get pv                                 
```

### Delete Per Environment
- DEV
```bash

# Delete namespace - order-service, notification-service, ui (pods, Services, Secrets)
kubectl delete -f argocd/order-service/dev.yaml \
               -f argocd/notification-service/dev.yaml \
               -f argocd/ui/dev.yaml

# Delete backends - they live in the `data` namespace
k delete -f infra/k8s/data/orders-db/dev.yaml \
               -f infra/k8s/data/notifications-db/dev.yaml \
               -f infra/k8s/data/rabbitmq/dev.yaml

kubectl -n data delete pvc -l app=orders-db-dev
kubectl -n data delete pvc -l app=notifications-db-dev
kubectl -n data delete pvc -l app=rabbitmq-dev

kubectl delete namespace dev

```

- PROD
```bash

kubectl delete -f argocd/order-service/prod.yaml -f argocd/notification-service/prod.yaml -f argocd/ui/prod.yaml

kubectl delete -f infra/k8s/data/orders-db/prod.yaml -f infra/k8s/data/notifications-db/prod.yaml -f infra/k8s/data/rabbitmq/prod.yaml

kubectl -n data delete pvc -l app=orders-db-prod
kubectl -n data delete pvc -l app=notifications-db-prod
kubectl -n data delete pvc -l app=rabbitmq-prod

kubectl delete namespace prod

```

# Scripts
- run: `./latest-dockerhub-tags.sh`
- Prints the newest image tag on Docker Hub for each service next to what `values-prod.yaml` pins, so you don't have to check three repos by hand.
- use flag `--bump` rewrites the values files to the newest tags — review the diff, commit, and Argo deploys.