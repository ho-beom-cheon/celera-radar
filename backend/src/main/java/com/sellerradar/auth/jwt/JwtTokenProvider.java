package com.sellerradar.auth.jwt;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.plan.domain.Plan;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.domain.UserRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtTokenProvider {
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final int MIN_SECRET_BYTES = 32;
	private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
	};

	private final JwtProperties properties;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	@Autowired
	public JwtTokenProvider(JwtProperties properties, ObjectMapper objectMapper) {
		this(properties, objectMapper, Clock.systemUTC());
	}

	JwtTokenProvider(JwtProperties properties, ObjectMapper objectMapper, Clock clock) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	public String issueAccessToken(User user) {
		return issueToken(user);
	}

	public JwtClaims parseAccessToken(String token) {
		return parseToken(token);
	}

	private String issueToken(User user) {
		Instant now = Instant.now(clock);
		Instant expiresAt = now.plus(properties.accessTokenTtl());
		Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", user.getId().toString());
		claims.put("email", user.getEmail());
		claims.put("role", user.getRole().name());
		claims.put("plan", user.getPlanCode().name());
		claims.put("type", TokenType.ACCESS.name());
		claims.put("iat", now.getEpochSecond());
		claims.put("exp", expiresAt.getEpochSecond());

		String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(claims);
		return unsignedToken + "." + sign(unsignedToken);
	}

	private JwtClaims parseToken(String token) {
		String[] parts = token == null ? new String[0] : token.split("\\.");
		if (parts.length != 3) {
			throw invalidToken();
		}
		String unsignedToken = parts[0] + "." + parts[1];
		if (!MessageDigest.isEqual(sign(unsignedToken).getBytes(StandardCharsets.UTF_8),
				parts[2].getBytes(StandardCharsets.UTF_8))) {
			throw invalidToken();
		}
		Map<String, Object> claims = decodeClaims(parts[1]);
		TokenType tokenType = enumClaim(claims, "type", TokenType.class);
		if (tokenType != TokenType.ACCESS) {
			throw invalidToken();
		}
		long expiresAt = longClaim(claims, "exp");
		if (Instant.now(clock).getEpochSecond() >= expiresAt) {
			throw invalidToken();
		}
		return new JwtClaims(
				Long.valueOf(stringClaim(claims, "sub")),
				stringClaim(claims, "email"),
				enumClaim(claims, "role", UserRole.class),
				enumClaim(claims, "plan", Plan.class),
				tokenType
		);
	}

	private Map<String, Object> decodeClaims(String payload) {
		try {
			byte[] decoded = Base64.getUrlDecoder().decode(payload);
			return objectMapper.readValue(decoded, CLAIMS_TYPE);
		} catch (IllegalArgumentException | JacksonException exception) {
			throw invalidToken();
		}
	}

	private String base64UrlJson(Map<String, Object> value) {
		try {
			byte[] json = objectMapper.writeValueAsBytes(value);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
		} catch (JacksonException exception) {
			throw new IllegalStateException("JWT JSON 직렬화에 실패했습니다.", exception);
		}
	}

	private String sign(String unsignedToken) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(signingKey(), HMAC_ALGORITHM));
			return Base64.getUrlEncoder()
					.withoutPadding()
					.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("JWT 서명에 실패했습니다.", exception);
		}
	}

	private byte[] signingKey() {
		String secret = properties.secret();
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("JWT_SECRET 환경변수가 필요합니다.");
		}
		byte[] key = secret.getBytes(StandardCharsets.UTF_8);
		if (key.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("JWT_SECRET은 최소 32바이트 이상이어야 합니다.");
		}
		return key;
	}

	private String stringClaim(Map<String, Object> claims, String name) {
		Object value = claims.get(name);
		if (value instanceof String stringValue && !stringValue.isBlank()) {
			return stringValue;
		}
		throw invalidToken();
	}

	private long longClaim(Map<String, Object> claims, String name) {
		Object value = claims.get(name);
		if (value instanceof Number numberValue) {
			return numberValue.longValue();
		}
		throw invalidToken();
	}

	private <T extends Enum<T>> T enumClaim(
			Map<String, Object> claims,
			String name,
			Class<T> enumType
	) {
		try {
			return Enum.valueOf(enumType, stringClaim(claims, name));
		} catch (IllegalArgumentException exception) {
			throw invalidToken();
		}
	}

	private BusinessException invalidToken() {
		return new BusinessException(ErrorCode.AUTH_REQUIRED);
	}
}
