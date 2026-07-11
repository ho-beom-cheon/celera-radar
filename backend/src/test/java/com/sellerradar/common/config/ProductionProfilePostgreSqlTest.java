package com.sellerradar.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.SellerRadarApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ProductionProfilePostgreSqlTest {
	private static final String VALID_JWT_SECRET = "prod-jwt-secret-with-at-least-32-bytes";

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@Test
	void startsProductionContextWithSafeSettingsAndPostgreSql() {
		SpringApplication application = new SpringApplication(SellerRadarApplication.class);

		try (ConfigurableApplicationContext context = application.run(
				"--server.port=0",
				"--spring.profiles.active=prod",
				"--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
				"--spring.datasource.driver-class-name=" + POSTGRES.getDriverClassName(),
				"--spring.datasource.username=" + POSTGRES.getUsername(),
				"--spring.datasource.password=" + POSTGRES.getPassword(),
				"--seller-radar.jwt.secret=" + VALID_JWT_SECRET
		)) {
			assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
					.isEqualTo("validate");
			assertThat(context.getEnvironment().getProperty("spring.flyway.baseline-on-migrate"))
					.isEqualTo("false");
			assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include"))
					.isEqualTo("health");
			assertThat(context.getBean(Flyway.class).info().current().getVersion().getVersion())
					.isEqualTo("013");
		}
	}
}
