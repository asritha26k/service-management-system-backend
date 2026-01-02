FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/*.jar identity-service.jar

EXPOSE ${SERVER_PORT:-8081}

ENTRYPOINT ["java", "-jar", "identity-service.jar"]
