package com.sellerradar.auth.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seller-radar.auth.rate-limit")
public record AuthRateLimitProperties(
		int loginAccountLimit,
		int loginIpLimit,
		Duration loginWindow,
		int signupAccountLimit,
		int signupIpLimit,
		Duration signupWindow,
		int passwordResetAccountLimit,
		int passwordResetIpLimit,
		Duration passwordResetWindow,
		int maxTrackedKeys
) {
	public Policy policy(AuthRateLimitAction action) {
		return switch (action) {
			case LOGIN -> new Policy(loginAccountLimit, loginIpLimit, loginWindow);
			case SIGNUP -> new Policy(signupAccountLimit, signupIpLimit, signupWindow);
			case PASSWORD_RESET -> new Policy(
					passwordResetAccountLimit,
					passwordResetIpLimit,
					passwordResetWindow
			);
		};
	}

	public record Policy(int accountLimit, int ipLimit, Duration window) {
	}
}
