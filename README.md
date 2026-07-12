# Kubernetes Helm Event Driven Microservices

# Start Locally
- web
  - `localhost:3000`
- order-service
    - `localhost:8080`
- notification-service
    - `localhost:8081`
- rabbitMQ dashboard:
  - http://localhost:15672/#/
  - guest/guest

## Docker
- point Docker at minikube's daemon so images are visible to the cluster (Section 10):
  - `eval $(minikube docker-env)`
- Build Docker Files
  - `docker build -t order-service:0.1.0 ./order-service`
  - `docker build -t notification-service:0.1.0 ./notification-service`
  - `docker build -t web:0.1.0 ./web`

## Docker Compose
- build both service JARs (runs tests too):
  - `mvn -f order-service/pom.xml clean package`
  - `mvn -f notification-service/pom.xml clean package`
- start up:
  - `docker compose up --build`