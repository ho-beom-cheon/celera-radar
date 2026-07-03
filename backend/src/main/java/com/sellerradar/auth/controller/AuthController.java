package com.sellerradar.auth.controller;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.LoginRequest;
import com.sellerradar.auth.dto.RefreshTokenRequest;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.auth.service.AuthService;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	public ApiResponse<AuthResponse> signup(
			@Valid @RequestBody SignupRequest request,
			HttpServletRequest servletRequest
	) {
		return ApiResponse.success(authService.signup(request), RequestContext.requestId(servletRequest));
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest servletRequest
	) {
		return ApiResponse.success(authService.login(request), RequestContext.requestId(servletRequest));
	}

	@PostMapping("/refresh")
	public ApiResponse<AuthResponse> refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest servletRequest
	) {
		return ApiResponse.success(authService.refresh(request), RequestContext.requestId(servletRequest));
	}
}
