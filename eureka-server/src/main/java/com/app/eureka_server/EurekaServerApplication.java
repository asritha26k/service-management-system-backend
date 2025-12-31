package com.app.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//eureka dashboard
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(EurekaServerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(EurekaServerApplication.class, args);
		logger.info("Eureka Server started successfully on port 8761");
		logger.info("Dashboard available at http://localhost:8761");
	}

}
