package com.sellerradar.auth.service;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.LoginRequest;
import com.sellerradar.auth.dto.RefreshTokenRequest;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.auth.jwt.JwtTokenProvider;
import com.sellerradar.auth.session.AuthSessionService;
import com.sellerradar.auth.session.IssuedRefreshSession;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthSessionService authSessionService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider,
			AuthSessionService authSessionService
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.authSessionService = authSessionService;
	}

	@Transactional
	public AuthResponse signup(SignupRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.DUPLICATED_EMAIL, ErrorCode.DUPLICATED_EMAIL.defaultMessage(), "email");
		}
		User user = userRepository.save(User.create(email, passwordEncoder.encode(request.password())));
		return toAuthResponse(authSessionService.issue(user));
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		User user = userRepository.findByEmail(email)
				.filter(User::isActive)
				.filter(foundUser -> passwordEncoder.matches(request.password(), foundUser.getPasswordHash()))
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
		return toAuthResponse(authSessionService.issue(user));
	}

	public AuthResponse refresh(RefreshTokenRequest request) {
		return toAuthResponse(authSessionService.rotate(request.refreshToken()));
	}

	public void logout(RefreshTokenRequest request) {
		authSessionService.logout(request.refreshToken());
	}

	public void logoutAll(Long userId) {
		authSessionService.logoutAll(userId);
	}

	private AuthResponse toAuthResponse(IssuedRefreshSession refreshSession) {
		User user = refreshSession.user();
		return new AuthResponse(
				user.getId(),
				user.getEmail(),
				user.getPlanCode(),
				jwtTokenProvider.issueAccessToken(user),
				refreshSession.refreshToken()
		);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
