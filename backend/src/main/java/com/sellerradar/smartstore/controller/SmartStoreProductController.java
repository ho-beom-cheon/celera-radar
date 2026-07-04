package com.sellerradar.smartstore.controller;

import com.sellerradar.auth.security.AuthenticatedUser;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.api.PageResponse;
import com.sellerradar.common.web.RequestContext;
import com.sellerradar.smartstore.dto.SmartStoreProductResponse;
import com.sellerradar.smartstore.dto.SmartStoreProductSyncResponse;
import com.sellerradar.smartstore.service.SmartStoreProductSyncService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/smartstore")
public class SmartStoreProductController {
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final SmartStoreProductSyncService productSyncService;

	public SmartStoreProductController(SmartStoreProductSyncService productSyncService) {
		this.productSyncService = productSyncService;
	}

	@PostMapping("/connections/{connectionId}/products/sync")
	public ApiResponse<SmartStoreProductSyncResponse> syncProducts(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long connectionId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				productSyncService.sync(user.userId(), connectionId),
				RequestContext.requestId(request)
		);
	}

	@GetMapping("/products")
	public ApiResponse<PageResponse<SmartStoreProductResponse>> listProducts(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
			@RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
			HttpServletRequest request
	) {
		Pageable pageable = PageRequest.of(
				Math.max(page, DEFAULT_PAGE),
				Math.min(Math.max(size, 1), MAX_SIZE),
				Sort.by(Sort.Direction.DESC, "lastSyncedAt")
		);
		return ApiResponse.success(
				PageResponse.from(productSyncService.list(user.userId(), pageable)),
				RequestContext.requestId(request)
		);
	}
}
