package com.sellerradar.alert.controller;

import com.sellerradar.alert.dto.AlertResponse;
import com.sellerradar.alert.dto.AlertRuleCreateRequest;
import com.sellerradar.alert.dto.AlertRuleResponse;
import com.sellerradar.alert.service.AlertService;
import com.sellerradar.auth.security.AuthenticatedUser;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.api.PageResponse;
import com.sellerradar.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AlertController {
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final AlertService alertService;

	public AlertController(AlertService alertService) {
		this.alertService = alertService;
	}

	@GetMapping("/api/v1/alerts")
	public ApiResponse<PageResponse<AlertResponse>> list(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
			@RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
			HttpServletRequest request
	) {
		Pageable pageable = PageRequest.of(
				Math.max(page, DEFAULT_PAGE),
				Math.min(Math.max(size, 1), MAX_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt")
		);
		return ApiResponse.success(
				PageResponse.from(alertService.list(user.userId(), pageable)),
				RequestContext.requestId(request)
		);
	}

	@PostMapping("/api/v1/alert-rules")
	public ApiResponse<AlertRuleResponse> createRule(
			@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody AlertRuleCreateRequest requestBody,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				alertService.createRule(user.userId(), requestBody),
				RequestContext.requestId(request)
		);
	}

	@PatchMapping("/api/v1/alerts/{alertId}/read")
	public ApiResponse<AlertResponse> markRead(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long alertId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				alertService.markRead(user.userId(), alertId),
				RequestContext.requestId(request)
		);
	}
}
