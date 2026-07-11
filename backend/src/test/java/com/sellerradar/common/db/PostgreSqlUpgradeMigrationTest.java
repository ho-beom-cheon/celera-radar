package com.sellerradar.common.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlUpgradeMigrationTest {
	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@Test
	void upgradesExistingV012SchemaToLatestVersion() throws Exception {
		Flyway v012 = flyway().target("12").load();
		v012.migrate();

		assertThat(v012.info().current().getVersion().getVersion()).isEqualTo("012");
		assertThat(tableExists("product_candidate")).isFalse();

		Flyway latest = flyway().load();
		latest.migrate();

		assertThat(latest.validateWithResult().validationSuccessful).isTrue();
		assertThat(latest.info().current().getVersion().getVersion()).isEqualTo("016");
		assertThat(tableExists("product_candidate")).isTrue();
		assertThat(tableExists("auth_security_events")).isTrue();
		assertThat(columnExists("wholesale_uploads", "raw_expires_at")).isTrue();
		assertThat(foreignKeyExists("fk_alert_candidate")).isTrue();
	}

	private org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
		return Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.baselineOnMigrate(false);
	}

	private boolean tableExists(String tableName) throws Exception {
		try (var connection = POSTGRES.createConnection("");
				var statement = connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
			statement.setString(1, "public." + tableName);
			try (var result = statement.executeQuery()) {
				return result.next() && result.getBoolean(1);
			}
		}
	}

	private boolean foreignKeyExists(String constraintName) throws Exception {
		try (var connection = POSTGRES.createConnection("");
				var statement = connection.prepareStatement(
						"SELECT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ?)")) {
			statement.setString(1, constraintName);
			try (var result = statement.executeQuery()) {
				return result.next() && result.getBoolean(1);
			}
		}
	}

	private boolean columnExists(String tableName, String columnName) throws Exception {
		try (var connection = POSTGRES.createConnection("");
				var statement = connection.prepareStatement(
						"SELECT EXISTS (SELECT 1 FROM information_schema.columns "
								+ "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?)")) {
			statement.setString(1, tableName);
			statement.setString(2, columnName);
			try (var result = statement.executeQuery()) {
				return result.next() && result.getBoolean(1);
			}
		}
	}
}
