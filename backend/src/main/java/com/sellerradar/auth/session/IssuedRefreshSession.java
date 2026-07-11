package com.sellerradar.auth.session;

import com.sellerradar.user.domain.User;

public record IssuedRefreshSession(
		User user,
		String refreshToken
) {
}
