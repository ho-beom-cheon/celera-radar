package com.sellerradar.wholesale.controller;

import com.sellerradar.auth.security.AuthenticatedUser;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.api.PageResponse;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.web.RequestContext;
import com.sellerradar.wholesale.domain.CsvEncoding;
import com.sellerradar.wholesale.dto.WholesaleColumnMappingRequest;
import com.sellerradar.wholesale.dto.WholesaleCandidateGenerationResponse;
import com.sellerradar.wholesale.dto.WholesaleFileResponse;
import com.sellerradar.wholesale.dto.WholesaleParseResponse;
import com.sellerradar.wholesale.dto.WholesaleProductRowResponse;
import com.sellerradar.wholesale.service.RawUploadLifecycleService;
import com.sellerradar.wholesale.service.WholesaleCandidateGenerationService;
import com.sellerradar.wholesale.service.WholesaleFileService;
import com.sellerradar.wholesale.service.WholesaleParsingService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/wholesale-files")
public class WholesaleFileController {
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final WholesaleFileService wholesaleFileService;
	private final WholesaleParsingService wholesaleParsingService;
	private final WholesaleCandidateGenerationService candidateGenerationService;
	private final RawUploadLifecycleService rawUploadLifecycleService;

	public WholesaleFileController(
			WholesaleFileService wholesaleFileService,
			WholesaleParsingService wholesaleParsingService,
			WholesaleCandidateGenerationService candidateGenerationService,
			RawUploadLifecycleService rawUploadLifecycleService
	) {
		this.wholesaleFileService = wholesaleFileService;
		this.wholesaleParsingService = wholesaleParsingService;
		this.candidateGenerationService = candidateGenerationService;
		this.rawUploadLifecycleService = rawUploadLifecycleService;
	}

	@PostMapping
	public ApiResponse<WholesaleFileResponse> upload(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam MultipartFile file,
			@RequestParam(defaultValue = "AUTO") CsvEncoding encoding,
			@RequestParam(required = false) String sourceName,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				wholesaleFileService.upload(user.userId(), file, encoding, sourceName),
				RequestContext.requestId(request)
		);
	}

	@GetMapping("/{fileId}")
	public ApiResponse<WholesaleFileResponse> get(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long fileId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				wholesaleFileService.get(user.userId(), fileId),
				RequestContext.requestId(request)
		);
	}

	@DeleteMapping("/{fileId}/raw")
	public ApiResponse<WholesaleFileResponse> deleteRawFile(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long fileId,
			HttpServletRequest request
	) {
		WholesaleFileResponse response = rawUploadLifecycleService.deleteForUser(user.userId(), fileId);
		if (response.rawDeletedAt() == null) {
			throw new BusinessException(ErrorCode.UPLOAD_RAW_DELETE_FAILED);
		}
		return ApiResponse.success(response, RequestContext.requestId(request));
	}

	@PostMapping("/{fileId}/column-mapping")
	public ApiResponse<WholesaleFileResponse> updateMapping(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long fileId,
			@Valid @RequestBody WholesaleColumnMappingRequest requestBody,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				wholesaleParsingService.updateMapping(user.userId(), fileId, requestBody),
				RequestContext.requestId(request)
		);
	}

	@PostMapping("/{fileId}/parse")
	public ApiResponse<WholesaleParseResponse> parse(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long fileId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				wholesaleParsingService.parse(user.userId(), fileId),
				RequestContext.requestId(request)
		);
	}

	@GetMapping("/{fileId}/rows")
	public ApiResponse<PageResponse<WholesaleProductRowResponse>> rows(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long fileId,
			@RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
			@RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
			HttpServletRequest request
	) {
		Pageable pageable = PageRequest.of(
				Math.max(page, DEFAULT_PAGE),
				Math.min(Math.max(size, 1), MAX_SIZE),
				Sort.by(Sort.Direction.ASC, "rowNo")
		);
		return ApiResponse.success(
				PageResponse.from(wholesaleParsingService.rows(user.userId(), fileId, pageable)),
				RequestContext.requestId(request)
		);
	}

	@PostMapping("/{fileId}/candidates")
	public ApiResponse<WholesaleCandidateGenerationResponse> generateCandidates(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long fileId,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				candidateGenerationService.generate(user.userId(), fileId),
				RequestContext.requestId(request)
		);
	}
}
