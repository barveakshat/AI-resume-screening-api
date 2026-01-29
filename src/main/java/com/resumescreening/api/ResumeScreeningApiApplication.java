package com.resumescreening.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ResumeScreeningApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeScreeningApiApplication.class, args);
	}
}