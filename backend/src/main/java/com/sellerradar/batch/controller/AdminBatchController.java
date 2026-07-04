package com.sellerradar.batch.controller;

import com.sellerradar.batch.dto.BatchJobHistoryResponse;
import com.sellerradar.batch.service.DailyTrendBatchService;
import com.sellerradar.batch.service.ShoppingSearchBatchService;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.api.PageResponse;
import com.sellerradar.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/batches")
public class AdminBatchController {
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final DailyTrendBatchService dailyTrendBatchService;
	private final ShoppingSearchBatchService shoppingSearchBatchService;

	public AdminBatchController(
			DailyTrendBatchService dailyTrendBatchService,
			ShoppingSearchBatchService shoppingSearchBatchService
	) {
		this.dailyTrendBatchService = dailyTrendBatchService;
		this.shoppingSearchBatchService = shoppingSearchBatchService;
	}

	@PostMapping("/datalab/run")
	public ApiResponse<BatchJobHistoryResponse> runDatalabTrend(HttpServletRequest request) {
		return ApiResponse.success(
				dailyTrendBatchService.runManualDatalabTrend(),
				RequestContext.requestId(request)
		);
	}

	@PostMapping("/shopping-search/run")
	public ApiResponse<BatchJobHistoryResponse> runShoppingSearch(HttpServletRequest request) {
		return ApiResponse.success(
				shoppingSearchBatchService.runManualShoppingSearch(),
				RequestContext.requestId(request)
		);
	}

	@GetMapping
	public ApiResponse<PageResponse<BatchJobHistoryResponse>> list(
			@RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
			@RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
			HttpServletRequest request
	) {
		Pageable pageable = PageRequest.of(
				Math.max(page, DEFAULT_PAGE),
				Math.min(Math.max(size, 1), MAX_SIZE),
				Sort.by(Sort.Direction.DESC, "startedAt")
		);
		return ApiResponse.success(
				PageResponse.from(shoppingSearchBatchService.list(pageable)),
				RequestContext.requestId(request)
		);
	}
}
