# Kubernetes Helm Event Driven Microservices

## Prerequisites
- install `minikube`
  - enable addons:
    - nginx ingress controller for the web UI: `minikube addons enable ingress` 
    - for kubectl top + HPA later: `minikube addons enable metrics-server`

---

## Demo
- start up `minikube`
  - `minikube start --cpus=4 --memory=6g --kubernetes-version=stable`

---

## Start Locally
- start apps
  - `docker compose` or `locally`

### Services
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
- `minikube start --cpus=4 --memory=6g --kubernetes-version=stable`
- `minikube start --cpus=4 --memory=7000 --kubernetes-version=stable`


---

## Helm
- install into the dev namespace:
  - helm install order-service ./order-service/helm -n dev

### Deployment Model
***This app used CI-driven Helm***
- CI-driven Helm pushes:
  - the pipeline runs helm upgrade and imperatively changes the cluster.
- Argo pulls and reconciles:
  - it watches Git and continuously forces the cluster to match. Crucially, Argo will revert anything it didn't do.

---

## Argo
- create namespace
  - `k create namespace argocd`
- install Argo CD
  - `k apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml`
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

### Argo delete cascades, removes everything 
- namespace: order-service, notification-service, ui — pods, Services, Secrets, all of it.
```angular2html

kubectl delete -f argocd/order-service/dev.yaml -f argocd/notification-service/dev.yaml -f argocd/ui/dev.yaml

kubectl delete -f argocd/order-service/prod.yaml -f argocd/notification-service/prod.yaml -f argocd/ui/prod.yaml
```