package com.sellerradar.auth.session;

import com.sellerradar.auth.jwt.JwtProperties;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.user.domain.User;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSessionService {
	private final AuthSessionRepository authSessionRepository;
	private final RefreshTokenCodec refreshTokenCodec;
	private final JwtProperties jwtProperties;

	public AuthSessionService(
			AuthSessionRepository authSessionRepository,
			RefreshTokenCodec refreshTokenCodec,
			JwtProperties jwtProperties
	) {
		this.authSessionRepository = authSessionRepository;
		this.refreshTokenCodec = refreshTokenCodec;
		this.jwtProperties = jwtProperties;
	}

	@Transactional
	public IssuedRefreshSession issue(User user) {
		GeneratedSession generated = generate(user, UUID.randomUUID(), now());
		return new IssuedRefreshSession(user, generated.refreshToken());
	}

	@Transactional(noRollbackFor = RefreshSessionRejectedException.class)
	public IssuedRefreshSession rotate(String refreshToken) {
		OffsetDateTime now = now();
		AuthSession current = authSessionRepository
				.findByTokenHashForUpdate(refreshTokenCodec.hash(refreshToken))
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

		if (current.isRotated()) {
			current.markReuseDetected(now);
			authSessionRepository.flush();
			authSessionRepository.revokeFamily(current.getFamilyId(), now);
			throw new RefreshSessionRejectedException();
		}
		if (current.isRevoked() || current.isExpired(now)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (!current.getUser().isActive() || current.getUser().getDeletedAt() != null) {
			authSessionRepository.revokeAllByUserId(current.getUser().getId(), now);
			throw new RefreshSessionRejectedException();
		}

		GeneratedSession replacement = generate(current.getUser(), current.getFamilyId(), now);
		current.rotateTo(replacement.session().getId(), now);
		return new IssuedRefreshSession(current.getUser(), replacement.refreshToken());
	}

	@Transactional
	public void logout(String refreshToken) {
		authSessionRepository.findByTokenHashForUpdate(refreshTokenCodec.hash(refreshToken))
				.ifPresent(session -> authSessionRepository.revokeFamily(session.getFamilyId(), now()));
	}

	@Transactional
	public void logoutAll(Long userId) {
		authSessionRepository.revokeAllByUserId(userId, now());
	}

	private GeneratedSession generate(User user, UUID familyId, OffsetDateTime issuedAt) {
		String refreshToken = refreshTokenCodec.generate();
		AuthSession session = AuthSession.create(
				user,
				familyId,
				refreshTokenCodec.hash(refreshToken),
				issuedAt.plus(jwtProperties.refreshTokenTtl()),
				issuedAt
		);
		authSessionRepository.save(session);
		return new GeneratedSession(session, refreshToken);
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private record GeneratedSession(AuthSession session, String refreshToken) {
	}
}
