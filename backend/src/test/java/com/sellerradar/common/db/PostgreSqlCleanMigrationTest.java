package com.sellerradar.common.db;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
		"spring.flyway.enabled=true",
		"spring.flyway.baseline-on-migrate=false",
		"spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlCleanMigrationTest {
	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
	}

	@Autowired
	private Flyway flyway;

	@Autowired
	private DataSource dataSource;

	@Test
	void migratesEmptyPostgreSqlAndValidatesHibernateSchema() throws Exception {
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("017");
		try (var connection = dataSource.getConnection();
				var statement = connection.prepareStatement("SELECT to_regclass('public.candidate_score')" );
				var result = statement.executeQuery()) {
			assertThat(result.next()).isTrue();
			assertThat(result.getString(1)).isEqualTo("candidate_score");
		}
		try (var connection = dataSource.getConnection();
				var statement = connection.prepareStatement("SELECT to_regclass('public.auth_security_events')");
				var result = statement.executeQuery()) {
			assertThat(result.next()).isTrue();
			assertThat(result.getString(1)).isEqualTo("auth_security_events");
		}
		try (var connection = dataSource.getConnection();
				var statement = connection.prepareStatement(
						"SELECT is_nullable FROM information_schema.columns "
								+ "WHERE table_name = 'wholesale_uploads' AND column_name = 'raw_expires_at'");
				var result = statement.executeQuery()) {
			assertThat(result.next()).isTrue();
			assertThat(result.getString(1)).isEqualTo("NO");
		}
		try (var connection = dataSource.getConnection();
				var statement = connection.prepareStatement(
						"SELECT EXISTS (SELECT 1 FROM information_schema.columns "
								+ "WHERE table_name = 'trend_snapshots' AND column_name = 'period')");
				var result = statement.executeQuery()) {
			assertThat(result.next()).isTrue();
			assertThat(result.getBoolean(1)).isFalse();
		}
	}
}
