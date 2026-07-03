package com.sellerradar.auth.security;

import com.sellerradar.plan.domain.Plan;
import com.sellerradar.user.domain.UserRole;

public record AuthenticatedUser(
		Long userId,
		String email,
		UserRole role,
		Plan plan
) {
}
