package com.sellerradar.keyword.controller;

import com.sellerradar.auth.security.AuthenticatedUser;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.api.PageResponse;
import com.sellerradar.common.web.RequestContext;
import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.dto.KeywordAnalysisResponse;
import com.sellerradar.keyword.dto.KeywordCreateRequest;
import com.sellerradar.keyword.dto.KeywordResponse;
import com.sellerradar.keyword.dto.KeywordUpdateRequest;
import com.sellerradar.keyword.dto.ShoppingSnapshotResponse;
import com.sellerradar.keyword.service.KeywordAnalysisService;
import com.sellerradar.keyword.service.KeywordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/keywords")
public class KeywordController {
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final KeywordService keywordService;
	private final KeywordAnalysisService keywordAnalysisService;

	public KeywordController(KeywordService keywordService, KeywordAnalysisService keywordAnalysisService) {
		this.keywordService = keywordService;
		this.keywordAnalysisService = keywordAnalysisService;
	}

	@GetMapping
	public ApiResponse<PageResponse<KeywordResponse>> list(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) AnalysisStatus analysisStatus,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
			@RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
			HttpServletRequest request
	) {
		Pageable pageable = PageRequest.of(
				Math.max(page, DEFAULT_PAGE),
				Math.min(Math.max(size, 1), MAX_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt")
		);
		PageResponse<KeywordResponse> response = PageResponse.from(keywordService.list(user.userId(), category, analysisStatus, pageable));
		return ApiResponse.success(response, RequestContext.requestId(request));
	}

	@GetMapping("/{keywordId}")
	public ApiResponse<KeywordResponse> detail(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long keywordId,
			HttpServletRequest request
	) {
		return ApiResponse.success(keywordService.get(user.userId(), keywordId), RequestContext.requestId(request));
	}

	@GetMapping("/{keywordId}/analysis")
	public ApiResponse<KeywordAnalysisResponse> analysis(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long keywordId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				keywordAnalysisService.getAnalysis(user.userId(), keywordId),
				RequestContext.requestId(request)
		);
	}

	@PostMapping("/{keywordId}/analyze/shopping")
	public ApiResponse<ShoppingSnapshotResponse> analyzeShopping(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long keywordId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				keywordAnalysisService.analyzeShopping(user.userId(), keywordId),
				RequestContext.requestId(request)
		);
	}

	@GetMapping("/{keywordId}/shopping-snapshot/latest")
	public ApiResponse<ShoppingSnapshotResponse> latestShoppingSnapshot(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long keywordId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				keywordAnalysisService.getLatestShoppingSnapshot(user.userId(), keywordId),
				RequestContext.requestId(request)
		);
	}

	@PostMapping
	public ApiResponse<KeywordResponse> create(
			@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody KeywordCreateRequest requestBody,
			HttpServletRequest request
	) {
		return ApiResponse.success(keywordService.create(user.userId(), requestBody), RequestContext.requestId(request));
	}

	@PutMapping("/{keywordId}")
	public ApiResponse<KeywordResponse> update(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long keywordId,
			@Valid @RequestBody KeywordUpdateRequest requestBody,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				keywordService.update(user.userId(), keywordId, requestBody),
				RequestContext.requestId(request)
		);
	}

	@DeleteMapping("/{keywordId}")
	public ApiResponse<Void> delete(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long keywordId,
			HttpServletRequest request
	) {
		keywordService.delete(user.userId(), keywordId);
		return ApiResponse.success(null, RequestContext.requestId(request));
	}
}
