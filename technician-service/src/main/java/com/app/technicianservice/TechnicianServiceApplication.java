package com.app.technicianservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.app.technicianservice")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.app.technicianservice.feign")
public class TechnicianServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechnicianServiceApplication.class, args);
    }
}
