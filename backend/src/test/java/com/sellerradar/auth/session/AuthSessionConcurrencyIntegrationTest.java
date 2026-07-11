package com.sellerradar.auth.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.LoginRequest;
import com.sellerradar.auth.dto.RefreshTokenRequest;
import com.sellerradar.auth.service.AuthService;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class AuthSessionConcurrencyIntegrationTest {
	private static final String EMAIL = "concurrent-refresh@example.com";
	private static final String PASSWORD = "password1234";

	@Autowired
	private AuthService authService;

	@Autowired
	private AuthSessionRepository authSessionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		authSessionRepository.deleteAllInBatch();
		userRepository.deleteAll();
		userRepository.save(User.create(EMAIL, passwordEncoder.encode(PASSWORD)));
	}

	@Test
	void concurrentRefreshAllowsOneRotationAndTreatsTheOtherAsReuse() throws Exception {
		AuthResponse initial = authService.login(new LoginRequest(EMAIL, PASSWORD));
		CountDownLatch start = new CountDownLatch(1);
		var executor = Executors.newFixedThreadPool(2);
		try {
			List<Future<RefreshAttempt>> futures = new ArrayList<>();
			for (int index = 0; index < 2; index++) {
				futures.add(executor.submit(() -> {
					start.await();
					try {
						return RefreshAttempt.success(authService.refresh(
								new RefreshTokenRequest(initial.refreshToken())
						));
					} catch (BusinessException exception) {
						return RefreshAttempt.failure(exception.errorCode());
					}
				}));
			}
			start.countDown();

			List<RefreshAttempt> attempts = new ArrayList<>();
			for (Future<RefreshAttempt> future : futures) {
				attempts.add(future.get());
			}

			assertThat(attempts).filteredOn(attempt -> attempt.response() != null).hasSize(1);
			assertThat(attempts)
					.filteredOn(attempt -> attempt.errorCode() == ErrorCode.INVALID_REFRESH_TOKEN)
					.hasSize(1);

			AuthResponse rotated = attempts.stream()
					.map(RefreshAttempt::response)
					.filter(response -> response != null)
					.findFirst()
					.orElseThrow();
			assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(rotated.refreshToken())))
					.isInstanceOf(BusinessException.class)
					.extracting(exception -> ((BusinessException) exception).errorCode())
					.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

			assertThat(authSessionRepository.findAll())
					.allSatisfy(session -> assertThat(session.getRevokedAt()).isNotNull());
			assertThat(authSessionRepository.findAll())
					.anySatisfy(session -> assertThat(session.getReuseDetectedAt()).isNotNull());
		} finally {
			executor.shutdownNow();
		}
	}

	private record RefreshAttempt(AuthResponse response, ErrorCode errorCode) {
		static RefreshAttempt success(AuthResponse response) {
			return new RefreshAttempt(response, null);
		}

		static RefreshAttempt failure(ErrorCode errorCode) {
			return new RefreshAttempt(null, errorCode);
		}
	}
}
