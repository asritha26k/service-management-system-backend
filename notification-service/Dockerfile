FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/*.jar notification-service.jar

EXPOSE ${SERVER_PORT:-8082}

ENTRYPOINT ["java", "-jar", "notification-service.jar"]
