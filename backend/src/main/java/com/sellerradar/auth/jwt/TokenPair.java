package com.sellerradar.auth.jwt;

public record TokenPair(
		String accessToken,
		String refreshToken
) {
}
