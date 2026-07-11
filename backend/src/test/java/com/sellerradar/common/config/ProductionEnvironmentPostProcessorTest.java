package com.sellerradar.common.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellerradar.SellerRadarApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class ProductionEnvironmentPostProcessorTest {
	private static final String VALID_JWT_SECRET = "prod-jwt-secret-with-at-least-32-bytes";
	private final ProductionEnvironmentPostProcessor validator = new ProductionEnvironmentPostProcessor();

	@Test
	void ignoresNonProductionProfiles() {
		assertThatCode(() -> validator.postProcessEnvironment(new MockEnvironment(), null))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsMissingProductionPropertyWithoutExposingOtherSecrets() {
		String databasePassword = "private-database-password";
		MockEnvironment environment = productionEnvironment()
				.withProperty("spring.datasource.password", databasePassword);

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("spring.datasource.url")
				.hasMessageNotContaining(databasePassword);
	}

	@Test
	void rejectsNonPostgreSqlDatabase() {
		MockEnvironment environment = validProductionEnvironment()
				.withProperty("spring.datasource.url", "jdbc:h2:mem:prod");

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("spring.datasource.url")
				.hasMessageNotContaining("jdbc:h2:mem:prod");
	}

	@Test
	void rejectsDevelopmentDatabaseCredentials() {
		MockEnvironment environment = validProductionEnvironment()
				.withProperty("spring.datasource.username", "seller")
				.withProperty("spring.datasource.password", "seller");

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("development defaults")
				.hasMessageNotContaining("seller/seller");
	}

	@Test
	void rejectsPlaceholderDatabasePasswordWithoutExposingIt() {
		String placeholder = "replace-with-production-database-password";
		MockEnvironment environment = validProductionEnvironment()
				.withProperty("spring.datasource.password", placeholder);

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("placeholder")
				.hasMessageNotContaining(placeholder);
	}

	@Test
	void rejectsShortJwtSecretWithoutExposingIt() {
		String shortSecret = "short-private-secret";
		MockEnvironment environment = validProductionEnvironment()
				.withProperty("seller-radar.jwt.secret", shortSecret);

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 32 bytes")
				.hasMessageNotContaining(shortSecret);
	}

	@Test
	void rejectsPlaceholderJwtSecretWithoutExposingIt() {
		String placeholder = "replace-with-production-secret-value";
		MockEnvironment environment = validProductionEnvironment()
				.withProperty("seller-radar.jwt.secret", placeholder);

		assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("placeholder")
				.hasMessageNotContaining(placeholder);
	}

	@Test
	void acceptsValidProductionConfiguration() {
		assertThatCode(() -> validator.postProcessEnvironment(validProductionEnvironment(), null))
				.doesNotThrowAnyException();
	}

	@Test
	void registeredProcessorStopsProductionBootWithDevelopmentCredentials() {
		SpringApplication application = new SpringApplication(SellerRadarApplication.class);

		assertThatThrownBy(() -> application.run(
				"--spring.profiles.active=prod",
				"--spring.datasource.url=jdbc:postgresql://db.internal:5432/seller_radar",
				"--spring.datasource.username=seller",
				"--spring.datasource.password=seller",
				"--seller-radar.jwt.secret=" + VALID_JWT_SECRET
		))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("development defaults")
				.hasMessageNotContaining(VALID_JWT_SECRET);
	}

	@Test
	void registeredProcessorStopsProductionBootWithoutLeakingShortJwtSecret() {
		String shortSecret = "private-short-jwt-secret";
		SpringApplication application = new SpringApplication(SellerRadarApplication.class);

		assertThatThrownBy(() -> application.run(
				"--spring.profiles.active=prod",
				"--spring.datasource.url=jdbc:postgresql://db.internal:5432/seller_radar",
				"--spring.datasource.username=seller_app",
				"--spring.datasource.password=unique-private-database-password",
				"--seller-radar.jwt.secret=" + shortSecret
		))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 32 bytes")
				.hasMessageNotContaining(shortSecret);
	}

	private MockEnvironment productionEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");
		return environment;
	}

	private MockEnvironment validProductionEnvironment() {
		return productionEnvironment()
				.withProperty("spring.datasource.url", "jdbc:postgresql://db.internal:5432/seller_radar")
				.withProperty("spring.datasource.username", "seller_app")
				.withProperty("spring.datasource.password", "unique-private-database-password")
				.withProperty("seller-radar.jwt.secret", VALID_JWT_SECRET);
	}
}
