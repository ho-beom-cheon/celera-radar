package com.sellerradar.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sellerradar.auth.securityevent.AuthSecurityEventService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AuthRateLimitServiceTest {
	private static final Duration WINDOW = Duration.ofMinutes(15);

	@Test
	void blocksAfterAccountLimitAndRecordsOnlyTheFirstBlockedAttempt() {
		MutableClock clock = new MutableClock(Instant.parse("2026-07-12T00:00:00Z"));
		AuthSecurityEventService eventService = mock(AuthSecurityEventService.class);
		AuthRateLimitService service = service(properties(1, 10, 100), eventService, clock);

		assertThatCode(() -> service.check(AuthRateLimitAction.LOGIN, "192.0.2.10", "User@Example.com"))
				.doesNotThrowAnyException();
		assertThatThrownBy(() -> service.check(AuthRateLimitAction.LOGIN, "192.0.2.10", "user@example.com"))
				.isInstanceOf(AuthRateLimitExceededException.class);
		assertThatThrownBy(() -> service.check(AuthRateLimitAction.LOGIN, "192.0.2.10", "user@example.com"))
				.isInstanceOf(AuthRateLimitExceededException.class);

		verify(eventService, times(1)).recordRateLimited(
				any(AuthRateLimitAction.class), any(), any(), anyLong()
		);
	}

	@Test
	void passwordResetUsesTheSamePolicyEngine() {
		MutableClock clock = new MutableClock(Instant.parse("2026-07-12T00:00:00Z"));
		AuthRateLimitService service = service(properties(1, 10, 100), mock(AuthSecurityEventService.class), clock);

		service.check(AuthRateLimitAction.PASSWORD_RESET, "192.0.2.20", "reset@example.com");

		assertThatThrownBy(() -> service.check(
				AuthRateLimitAction.PASSWORD_RESET,
				"192.0.2.20",
				"reset@example.com"
		)).isInstanceOf(AuthRateLimitExceededException.class);
	}

	@Test
	void expiredWindowsAreRemovedAndRequestsCanContinue() {
		MutableClock clock = new MutableClock(Instant.parse("2026-07-12T00:00:00Z"));
		AuthRateLimitService service = service(properties(1, 10, 100), mock(AuthSecurityEventService.class), clock);

		service.check(AuthRateLimitAction.LOGIN, "192.0.2.30", "expired@example.com");
		clock.advance(WINDOW.plusSeconds(1));

		assertThatCode(() -> service.check(AuthRateLimitAction.LOGIN, "192.0.2.30", "expired@example.com"))
				.doesNotThrowAnyException();
	}

	@Test
	void refusesNewKeysWhenTheBoundIsReached() {
		MutableClock clock = new MutableClock(Instant.parse("2026-07-12T00:00:00Z"));
		AuthRateLimitService service = service(properties(10, 10, 2), mock(AuthSecurityEventService.class), clock);
		service.check(AuthRateLimitAction.LOGIN, "192.0.2.40", "first@example.com");

		assertThat(service.trackedKeyCount()).isEqualTo(2);
		assertThatThrownBy(() -> service.check(AuthRateLimitAction.LOGIN, "192.0.2.41", "second@example.com"))
				.isInstanceOf(AuthRateLimitExceededException.class);
		assertThat(service.trackedKeyCount()).isEqualTo(2);
	}

	private AuthRateLimitService service(
			AuthRateLimitProperties properties,
			AuthSecurityEventService eventService,
			Clock clock
	) {
		return new AuthRateLimitService(properties, new SecurityValueHasher(), eventService, clock);
	}

	private AuthRateLimitProperties properties(int accountLimit, int ipLimit, int maxTrackedKeys) {
		return new AuthRateLimitProperties(
				accountLimit, ipLimit, WINDOW,
				accountLimit, ipLimit, WINDOW,
				accountLimit, ipLimit, WINDOW,
				maxTrackedKeys
		);
	}

	private static final class MutableClock extends Clock {
		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("UTC");
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
