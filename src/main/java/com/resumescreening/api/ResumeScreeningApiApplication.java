package com.resumescreening.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class ResumeScreeningApiApplication {

	@Autowired
	private Environment env;

	public static void main(String[] args) {
		SpringApplication.run(ResumeScreeningApiApplication.class, args);
	}

	@PostConstruct
	public void init() {
		System.out.println("=== AWS Configuration Check ===");
		System.out.println("Bucket: " + env.getProperty("aws.s3.bucket-name"));
		System.out.println("Region: " + env.getProperty("aws.s3.region"));
		System.out.println("Access Key: " + (env.getProperty("aws.s3.access-key") != null ? "Set ✓" : "Missing ✗"));
		System.out.println("Secret Key: " + (env.getProperty("aws.s3.secret-key") != null ? "Set ✓" : "Missing ✗"));
		System.out.println("===============================");
	}
}