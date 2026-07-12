FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# non-root user (matches the restricted Pod Security we enforce in prod)
RUN addgroup -S app && adduser -S app -G app
USER app

## sensible heap default; Helm overrides JAVA_OPTS per namespace (dev/prod).
#ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT exec java $JAVA_OPTS -jar app.jar