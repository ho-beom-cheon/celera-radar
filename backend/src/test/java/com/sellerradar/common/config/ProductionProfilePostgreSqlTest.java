package com.sellerradar.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellerradar.SellerRadarApplication;
import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.LoginRequest;
import com.sellerradar.auth.dto.RefreshTokenRequest;
import com.sellerradar.auth.service.AuthService;
import com.sellerradar.auth.session.AuthSessionRepository;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
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
			assertThat(context.getEnvironment().getProperty("seller-radar.web-security.csp-enforce"))
					.isEqualTo("true");
			assertThat(context.getEnvironment().getProperty("seller-radar.web-security.allowed-origins"))
					.isEmpty();
		assertThat(context.getEnvironment().getProperty("seller-radar.external.naver.mode"))
				.isEqualTo("DISABLED");
		assertThat(context.getEnvironment().getProperty("seller-radar.features.smartstore-mock-enabled"))
				.isEqualTo("false");
			assertThat(context.getBean(Flyway.class).info().current().getVersion().getVersion())
					.isEqualTo("017");

			assertRefreshReuseRevokesFamily(context);
		}
	}

	private void assertRefreshReuseRevokesFamily(ConfigurableApplicationContext context) {
		AuthSessionRepository sessionRepository = context.getBean(AuthSessionRepository.class);
		UserRepository userRepository = context.getBean(UserRepository.class);
		PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
		AuthService authService = context.getBean(AuthService.class);
		String email = "postgres-refresh@example.com";
		String password = "password1234";

		sessionRepository.deleteAllInBatch();
		userRepository.deleteAll();
		userRepository.save(User.create(email, passwordEncoder.encode(password)));

		AuthResponse initial = authService.login(new LoginRequest(email, password));
		AuthResponse rotated = authService.refresh(new RefreshTokenRequest(initial.refreshToken()));

		assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(initial.refreshToken())))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).errorCode())
				.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
		assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(rotated.refreshToken())))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).errorCode())
				.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
		assertThat(sessionRepository.findAll())
				.allSatisfy(session -> assertThat(session.getRevokedAt()).isNotNull());
	}
}
