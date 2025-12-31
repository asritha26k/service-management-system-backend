package com.app.config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//config server port 8888
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(ConfigServerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
		logger.info("Config Server started successfully on port 8888");
		logger.info("Git repository configured for centralized configuration management");
	}

}
