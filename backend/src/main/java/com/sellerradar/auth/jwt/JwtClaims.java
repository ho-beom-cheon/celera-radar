package com.sellerradar.auth.jwt;

import com.sellerradar.plan.domain.Plan;
import com.sellerradar.user.domain.UserRole;

public record JwtClaims(
		Long userId,
		String email,
		UserRole role,
		Plan plan,
		TokenType type
) {
}
