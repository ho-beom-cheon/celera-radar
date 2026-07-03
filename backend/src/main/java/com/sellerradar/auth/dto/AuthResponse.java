package com.sellerradar.auth.dto;

import com.sellerradar.plan.domain.Plan;

public record AuthResponse(
		Long userId,
		String email,
		Plan plan,
		String accessToken,
		String refreshToken
) {
}
