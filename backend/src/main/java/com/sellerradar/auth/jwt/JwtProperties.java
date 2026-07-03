package com.sellerradar.auth.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seller-radar.jwt")
public record JwtProperties(
		String secret,
		Duration accessTokenTtl,
		Duration refreshTokenTtl
) {
}
