package com.sellerradar.smartstore.controller;

import com.sellerradar.auth.security.AuthenticatedUser;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.api.PageResponse;
import com.sellerradar.common.web.RequestContext;
import com.sellerradar.smartstore.dto.StoreProductCostResponse;
import com.sellerradar.smartstore.dto.StoreProductCostUpsertRequest;
import com.sellerradar.smartstore.dto.SmartStoreProductResponse;
import com.sellerradar.smartstore.dto.SmartStoreProductSyncResponse;
import com.sellerradar.smartstore.service.StoreProductCostService;
import com.sellerradar.smartstore.service.SmartStoreProductSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/smartstore")
public class SmartStoreProductController {
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final SmartStoreProductSyncService productSyncService;
	private final StoreProductCostService productCostService;

	public SmartStoreProductController(
			SmartStoreProductSyncService productSyncService,
			StoreProductCostService productCostService
	) {
		this.productSyncService = productSyncService;
		this.productCostService = productCostService;
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

	@PutMapping("/products/{productId}/cost")
	public ApiResponse<StoreProductCostResponse> upsertCost(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long productId,
			@Valid @RequestBody StoreProductCostUpsertRequest requestBody,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				productCostService.upsert(user.userId(), productId, requestBody),
				RequestContext.requestId(request)
		);
	}

	@GetMapping("/products/{productId}/cost")
	public ApiResponse<StoreProductCostResponse> getCost(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long productId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				productCostService.get(user.userId(), productId),
				RequestContext.requestId(request)
		);
	}
}
