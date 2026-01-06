@echo off
echo Starting Service Management System Backend...

echo Starting Config Server
java -jar config-server\target\config-server-1.0.0.jar

echo Starting Eureka Server
java -jar eureka-server\target\eureka-server-1.0.0.jar

echo Starting API Gateway
java -jar api-gateway\target\api-gateway-1.0.0.jar

echo Starting Identity Service
java -jar identity-service\target\identity-service-1.0.0.jar

echo Starting Notification Service
java -jar notification-service\target\notification-service-1.0.0.jar

echo Starting Service Operations Service
java -jar service-operations-service\target\service-operations-service-1.0.0.jar

echo Starting Technician Service
java -jar technician-service\target\technician-service-1.0.0.jar
