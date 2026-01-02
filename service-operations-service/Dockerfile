FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/*.jar service-operations-service.jar

EXPOSE ${SERVER_PORT:-8083}

ENTRYPOINT ["java", "-jar", "service-operations-service.jar"]
