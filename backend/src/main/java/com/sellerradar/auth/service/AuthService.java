package com.sellerradar.auth.service;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.LoginRequest;
import com.sellerradar.auth.dto.RefreshTokenRequest;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.auth.jwt.JwtClaims;
import com.sellerradar.auth.jwt.JwtTokenProvider;
import com.sellerradar.auth.jwt.TokenPair;
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

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Transactional
	public AuthResponse signup(SignupRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.DUPLICATED_EMAIL, ErrorCode.DUPLICATED_EMAIL.defaultMessage(), "email");
		}
		User user = userRepository.save(User.create(email, passwordEncoder.encode(request.password())));
		return toAuthResponse(user, jwtTokenProvider.issueTokenPair(user));
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		User user = userRepository.findByEmail(email)
				.filter(foundUser -> passwordEncoder.matches(request.password(), foundUser.getPasswordHash()))
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
		return toAuthResponse(user, jwtTokenProvider.issueTokenPair(user));
	}

	@Transactional(readOnly = true)
	public AuthResponse refresh(RefreshTokenRequest request) {
		JwtClaims claims = jwtTokenProvider.parseRefreshToken(request.refreshToken());
		User user = userRepository.findById(claims.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
		return toAuthResponse(user, jwtTokenProvider.issueTokenPair(user));
	}

	private AuthResponse toAuthResponse(User user, TokenPair tokenPair) {
		return new AuthResponse(
				user.getId(),
				user.getEmail(),
				user.getPlanCode(),
				tokenPair.accessToken(),
				tokenPair.refreshToken()
		);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
