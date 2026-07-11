package com.sellerradar.auth.controller;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.LoginRequest;
import com.sellerradar.auth.dto.RefreshTokenRequest;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.auth.ratelimit.AuthRateLimitAction;
import com.sellerradar.auth.ratelimit.AuthRateLimitService;
import com.sellerradar.auth.security.AuthenticatedUser;
import com.sellerradar.auth.service.AuthService;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	private final AuthService authService;
	private final AuthRateLimitService authRateLimitService;

	public AuthController(AuthService authService, AuthRateLimitService authRateLimitService) {
		this.authService = authService;
		this.authRateLimitService = authRateLimitService;
	}

	@PostMapping("/signup")
	public ApiResponse<AuthResponse> signup(
			@Valid @RequestBody SignupRequest request,
			HttpServletRequest servletRequest
	) {
		authRateLimitService.check(AuthRateLimitAction.SIGNUP, servletRequest.getRemoteAddr(), request.email());
		return ApiResponse.success(authService.signup(request), RequestContext.requestId(servletRequest));
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest servletRequest
	) {
		authRateLimitService.check(AuthRateLimitAction.LOGIN, servletRequest.getRemoteAddr(), request.email());
		return ApiResponse.success(authService.login(request), RequestContext.requestId(servletRequest));
	}

	@PostMapping("/refresh")
	public ApiResponse<AuthResponse> refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest servletRequest
	) {
		return ApiResponse.success(authService.refresh(request), RequestContext.requestId(servletRequest));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest servletRequest
	) {
		authService.logout(request);
		return ApiResponse.success(null, RequestContext.requestId(servletRequest));
	}

	@PostMapping("/logout-all")
	public ApiResponse<Void> logoutAll(
			@AuthenticationPrincipal AuthenticatedUser user,
			HttpServletRequest servletRequest
	) {
		authService.logoutAll(user.userId());
		return ApiResponse.success(null, RequestContext.requestId(servletRequest));
	}
}
