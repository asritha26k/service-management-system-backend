FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/*.jar technician-service.jar

EXPOSE ${SERVER_PORT:-8084}

ENTRYPOINT ["java", "-jar", "technician-service.jar"]
