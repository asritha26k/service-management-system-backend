FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/*.jar config-server.jar

EXPOSE 8888

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=5 \
  CMD curl -f http://localhost:8888/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "config-server.jar"]
