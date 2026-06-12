package com.resumescreening.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@EnableCaching
public class ResumeScreeningApiApplication {

	public static void main(String[] args) {
		adaptRenderEnvironment();
		SpringApplication.run(ResumeScreeningApiApplication.class, args);
	}

	private static void adaptRenderEnvironment() {
		adaptDatabaseUrl();
		adaptRedisUrl();
	}

	private static void adaptDatabaseUrl() {
		String databaseUrl = System.getenv("DATABASE_URL");
		if (hasText(System.getenv("SPRING_DATASOURCE_URL")) || !hasText(databaseUrl)) {
			return;
		}

		if (databaseUrl.startsWith("jdbc:postgresql:")) {
			System.setProperty("spring.datasource.url", databaseUrl);
			return;
		}

		URI uri = URI.create(databaseUrl);
		String userInfo = uri.getUserInfo();
		String username = "";
		String password = "";
		if (hasText(userInfo)) {
			String[] parts = userInfo.split(":", 2);
			username = decode(parts[0]);
			if (parts.length > 1) {
				password = decode(parts[1]);
			}
		}

		int port = uri.getPort() == -1 ? 5432 : uri.getPort();
		String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
		if (hasText(uri.getQuery())) {
			jdbcUrl += "?" + uri.getQuery();
		}

		System.setProperty("spring.datasource.url", jdbcUrl);
		setIfMissing("SPRING_DATASOURCE_USERNAME", "spring.datasource.username", username);
		setIfMissing("SPRING_DATASOURCE_PASSWORD", "spring.datasource.password", password);
	}

	private static void adaptRedisUrl() {
		String redisUrl = System.getenv("REDIS_URL");
		if (hasText(System.getenv("SPRING_DATA_REDIS_HOST")) || !hasText(redisUrl)) {
			return;
		}

		URI uri = URI.create(redisUrl);
		int port = uri.getPort() == -1 ? 6379 : uri.getPort();
		String userInfo = uri.getUserInfo();
		if (hasText(userInfo)) {
			String[] parts = userInfo.split(":", 2);
			if (parts.length > 1) {
				System.setProperty("spring.data.redis.password", decode(parts[1]));
			}
		}

		System.setProperty("spring.data.redis.host", uri.getHost());
		System.setProperty("spring.data.redis.port", String.valueOf(port));
		System.setProperty("spring.data.redis.ssl.enabled", String.valueOf("rediss".equalsIgnoreCase(uri.getScheme())));
	}

	private static void setIfMissing(String environmentKey, String propertyKey, String value) {
		if (!hasText(System.getenv(environmentKey)) && hasText(value)) {
			System.setProperty(propertyKey, value);
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}
