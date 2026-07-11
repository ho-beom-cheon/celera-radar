package com.sellerradar.common.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public final class ProductionEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
	private static final int MIN_JWT_SECRET_BYTES = 32;
	private static final Set<String> UNSAFE_SECRET_VALUES = Set.of(
			"seller",
			"password",
			"secret",
			"changeme",
			"change-me",
			"replace-me"
	);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!environment.matchesProfiles("prod")) {
			return;
		}

		String databaseUrl = required(environment, "spring.datasource.url");
		String databaseUsername = required(environment, "spring.datasource.username");
		String databasePassword = required(environment, "spring.datasource.password");
		String jwtSecret = required(environment, "seller-radar.jwt.secret");

		if (!databaseUrl.startsWith("jdbc:postgresql://")) {
			throw invalid("spring.datasource.url", "must use PostgreSQL in production");
		}
		if (isUnsafe(databaseUsername) && isUnsafe(databasePassword)) {
			throw invalid("spring.datasource credentials", "must not use development defaults");
		}
		if (isUnsafe(databasePassword) || containsPlaceholderMarker(databasePassword)) {
			throw invalid("spring.datasource.password", "must not use a default or placeholder value");
		}
		if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
			throw invalid("seller-radar.jwt.secret", "must be at least 32 bytes");
		}
		if (isUnsafe(jwtSecret) || containsPlaceholderMarker(jwtSecret)) {
			throw invalid("seller-radar.jwt.secret", "must not use a placeholder value");
		}
	}

	@Override
	public int getOrder() {
		return ConfigDataEnvironmentPostProcessor.ORDER + 1;
	}

	private String required(ConfigurableEnvironment environment, String propertyName) {
		try {
			String value = environment.getProperty(propertyName);
			if (value == null || value.isBlank()) {
				throw invalid(propertyName, "is required in production");
			}
			return value.strip();
		} catch (IllegalArgumentException exception) {
			throw invalid(propertyName, "is required in production");
		}
	}

	private boolean isUnsafe(String value) {
		return UNSAFE_SECRET_VALUES.contains(value.strip().toLowerCase(Locale.ROOT));
	}

	private boolean containsPlaceholderMarker(String value) {
		String normalized = value.toLowerCase(Locale.ROOT);
		return normalized.contains("replace-with")
				|| normalized.contains("test-only")
				|| normalized.contains("example")
				|| normalized.contains("placeholder");
	}

	private IllegalStateException invalid(String propertyName, String reason) {
		return new IllegalStateException("Invalid production configuration: " + propertyName + " " + reason + ".");
	}
}
