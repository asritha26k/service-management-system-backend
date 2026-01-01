package com.app.service_operations_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ServiceOperationsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceOperationsServiceApplication.class, args);
	}

}
