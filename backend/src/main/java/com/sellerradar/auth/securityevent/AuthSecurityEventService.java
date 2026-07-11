package com.sellerradar.auth.securityevent;

import com.sellerradar.auth.ratelimit.AuthRateLimitAction;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSecurityEventService {
	private final AuthSecurityEventRepository repository;

	public AuthSecurityEventService(AuthSecurityEventRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordRateLimited(
			AuthRateLimitAction action,
			String subjectHash,
			String networkHash,
			long retryAfterSeconds
	) {
		repository.save(AuthSecurityEvent.rateLimited(
				action,
				subjectHash,
				networkHash,
				Math.toIntExact(retryAfterSeconds),
				OffsetDateTime.now(ZoneOffset.UTC)
		));
	}
}
