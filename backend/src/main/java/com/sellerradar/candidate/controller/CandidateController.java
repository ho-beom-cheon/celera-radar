package com.sellerradar.candidate.controller;

import com.sellerradar.auth.security.AuthenticatedUser;
import com.sellerradar.candidate.domain.CandidateSourceType;
import com.sellerradar.candidate.dto.CandidateDetailResponse;
import com.sellerradar.candidate.dto.CandidateListResponse;
import com.sellerradar.candidate.service.CandidateService;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.api.PageResponse;
import com.sellerradar.common.web.RequestContext;
import com.sellerradar.scoring.CandidateGrade;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
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
@RequestMapping("/api/v1/candidates")
public class CandidateController {
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final CandidateService candidateService;

	public CandidateController(CandidateService candidateService) {
		this.candidateService = candidateService;
	}

	@GetMapping
	public ApiResponse<PageResponse<CandidateListResponse>> list(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(required = false) CandidateGrade grade,
			@RequestParam(required = false) CategoryCode categoryCode,
			@RequestParam(required = false) Integer minScore,
			@RequestParam(required = false) BigDecimal minMarginRate,
			@RequestParam(name = "source", required = false) CandidateSourceType source,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
			@RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
			HttpServletRequest request
	) {
		Pageable pageable = PageRequest.of(
				Math.max(page, DEFAULT_PAGE),
				Math.min(Math.max(size, 1), MAX_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt")
		);
		PageResponse<CandidateListResponse> response = PageResponse.from(candidateService.list(
				user.userId(),
				grade,
				categoryCode,
				minScore,
				minMarginRate,
				source,
				pageable
		));
		return ApiResponse.success(response, RequestContext.requestId(request));
	}

	@GetMapping("/{candidateId}")
	public ApiResponse<CandidateDetailResponse> get(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long candidateId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				candidateService.get(user.userId(), candidateId),
				RequestContext.requestId(request)
		);
	}

	@PostMapping("/{candidateId}/save")
	public ApiResponse<CandidateDetailResponse> save(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long candidateId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				candidateService.save(user.userId(), candidateId),
				RequestContext.requestId(request)
		);
	}

	@PostMapping("/{candidateId}/exclude")
	public ApiResponse<CandidateDetailResponse> exclude(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long candidateId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				candidateService.exclude(user.userId(), candidateId),
				RequestContext.requestId(request)
		);
	}
}
