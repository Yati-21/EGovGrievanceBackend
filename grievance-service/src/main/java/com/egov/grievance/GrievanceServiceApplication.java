package com.egov.grievance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GrievanceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrievanceServiceApplication.class, args);
	}

}
