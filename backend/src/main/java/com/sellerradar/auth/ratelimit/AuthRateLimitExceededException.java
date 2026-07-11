package com.sellerradar.auth.ratelimit;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;

public class AuthRateLimitExceededException extends BusinessException {
	private final long retryAfterSeconds;

	public AuthRateLimitExceededException(long retryAfterSeconds) {
		super(ErrorCode.AUTH_RATE_LIMITED);
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long retryAfterSeconds() {
		return retryAfterSeconds;
	}
}
