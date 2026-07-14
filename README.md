# Kubernetes Helm Event Driven Microservices

## Prerequisites
- install `minikube`
  - enable addons:
    - nginx ingress controller for the web UI: `minikube addons enable ingress` 
    - for kubectl top + HPA later: `minikube addons enable metrics-server`

## Demo
- start up `minikube`
  - `minikube start --cpus=4 --memory=6g --kubernetes-version=stable`

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

## Docker
- point Docker at minikube's daemon so images are visible to the cluster (Section 10):
  - `eval $(minikube docker-env)`
- Build Docker Files
  - `docker build -t order-service:0.1.0 ./order-service`
  - `docker build -t notification-service:0.1.0 ./notification-service`
  - `docker build -t ui:0.1.0 ./ui`

## Docker Compose
- build both service JARs (runs tests too):
  - `mvn -f order-service/pom.xml clean package`
  - `mvn -f notification-service/pom.xml clean package`
- start up:
  - `docker compose up --build`

## Minikube
- start
- `minikube start --cpus=4 --memory=6g --kubernetes-version=stable`

## Deployment Model
***This app used CI-driven Helm***
- CI-driven Helm pushes:
  - the pipeline runs helm upgrade and imperatively changes the cluster.
- Argo pulls and reconciles:
  - it watches Git and continuously forces the cluster to match. Crucially, Argo will revert anything it didn't do.
