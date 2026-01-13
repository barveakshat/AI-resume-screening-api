package com.resumescreening.api;

import com.resumescreening.api.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class ResumeScreeningApiApplication implements CommandLineRunner {

	private final S3Service s3Service;

	public static void main(String[] args) {
		SpringApplication.run(ResumeScreeningApiApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Test S3 connection
		System.out.println("Testing S3 connection...");
		boolean connected = s3Service.fileExists("test-file.txt");
		System.out.println("S3 Connection: " + (connected ? "File exists" : "Ready to upload"));
	}
}