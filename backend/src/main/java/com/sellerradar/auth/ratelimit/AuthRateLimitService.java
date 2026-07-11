package com.sellerradar.auth.ratelimit;

import com.sellerradar.auth.ratelimit.AuthRateLimitProperties.Policy;
import com.sellerradar.auth.securityevent.AuthSecurityEventService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {
	private static final long CLEANUP_INTERVAL = 256;

	private final AuthRateLimitProperties properties;
	private final SecurityValueHasher hasher;
	private final AuthSecurityEventService securityEventService;
	private final Clock clock;
	private final ConcurrentHashMap<String, WindowState> counters = new ConcurrentHashMap<>();
	private final AtomicLong requestCount = new AtomicLong();

	@Autowired
	public AuthRateLimitService(
			AuthRateLimitProperties properties,
			SecurityValueHasher hasher,
			AuthSecurityEventService securityEventService
	) {
		this(properties, hasher, securityEventService, Clock.systemUTC());
	}

	AuthRateLimitService(
			AuthRateLimitProperties properties,
			SecurityValueHasher hasher,
			AuthSecurityEventService securityEventService,
			Clock clock
	) {
		this.properties = properties;
		this.hasher = hasher;
		this.securityEventService = securityEventService;
		this.clock = clock;
	}

	public void check(AuthRateLimitAction action, String remoteAddress, String account) {
		Instant now = clock.instant();
		cleanupIfNeeded(now);
		Policy policy = properties.policy(action);
		String subjectHash = hasher.hash(normalizeAccount(account));
		String networkHash = hasher.hash(normalizeNetwork(remoteAddress));

		LimitResult accountResult = consume(
				"account:" + action + ":" + subjectHash,
				policy.accountLimit(),
				policy.window(),
				now
		);
		LimitResult networkResult = consume(
				"network:" + action + ":" + networkHash,
				policy.ipLimit(),
				policy.window(),
				now
		);

		if (!accountResult.blocked() && !networkResult.blocked()) {
			return;
		}
		long retryAfter = Math.max(accountResult.retryAfterSeconds(), networkResult.retryAfterSeconds());
		if (accountResult.firstBlockedAttempt() || networkResult.firstBlockedAttempt()) {
			securityEventService.recordRateLimited(action, subjectHash, networkHash, retryAfter);
		}
		throw new AuthRateLimitExceededException(retryAfter);
	}

	void clear() {
		counters.clear();
	}

	int trackedKeyCount() {
		return counters.size();
	}

	private LimitResult consume(String key, int limit, Duration window, Instant now) {
		if (limit <= 0 || window == null || window.isZero() || window.isNegative()) {
			return LimitResult.blocked(Math.max(1, window == null ? 1 : window.toSeconds()), false);
		}
		if (!counters.containsKey(key) && counters.size() >= properties.maxTrackedKeys()) {
			cleanupExpired(now);
			if (counters.size() >= properties.maxTrackedKeys()) {
				return LimitResult.blocked(Math.max(1, window.toSeconds()), false);
			}
		}

		AtomicReference<LimitResult> result = new AtomicReference<>();
		counters.compute(key, (ignored, current) -> {
			if (current == null || !current.windowEnd().isAfter(now)) {
				result.set(LimitResult.allowed());
				return new WindowState(1, now.plus(window), false);
			}
			if (current.count() >= limit) {
				long retryAfter = retryAfterSeconds(now, current.windowEnd());
				result.set(LimitResult.blocked(retryAfter, !current.blockRecorded()));
				return new WindowState(current.count(), current.windowEnd(), true);
			}
			result.set(LimitResult.allowed());
			return new WindowState(current.count() + 1, current.windowEnd(), current.blockRecorded());
		});
		return result.get();
	}

	private void cleanupIfNeeded(Instant now) {
		if (requestCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
			cleanupExpired(now);
		}
	}

	private void cleanupExpired(Instant now) {
		counters.entrySet().removeIf(entry -> !entry.getValue().windowEnd().isAfter(now));
	}

	private long retryAfterSeconds(Instant now, Instant windowEnd) {
		long millis = Duration.between(now, windowEnd).toMillis();
		return Math.max(1, (millis + 999) / 1000);
	}

	private String normalizeAccount(String account) {
		return account == null ? "unknown" : account.strip().toLowerCase(Locale.ROOT);
	}

	private String normalizeNetwork(String remoteAddress) {
		return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.strip();
	}

	private record WindowState(int count, Instant windowEnd, boolean blockRecorded) {
	}

	private record LimitResult(boolean blocked, long retryAfterSeconds, boolean firstBlockedAttempt) {
		static LimitResult allowed() {
			return new LimitResult(false, 0, false);
		}

		static LimitResult blocked(long retryAfterSeconds, boolean firstBlockedAttempt) {
			return new LimitResult(true, retryAfterSeconds, firstBlockedAttempt);
		}
	}
}
