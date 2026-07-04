package com.sellerradar.wholesale.controller;

import com.sellerradar.auth.security.AuthenticatedUser;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.web.RequestContext;
import com.sellerradar.wholesale.domain.CsvEncoding;
import com.sellerradar.wholesale.dto.WholesaleUploadConfirmRequest;
import com.sellerradar.wholesale.dto.WholesaleUploadConfirmResponse;
import com.sellerradar.wholesale.dto.WholesaleUploadPreviewResponse;
import com.sellerradar.wholesale.service.WholesaleFileService;
import com.sellerradar.wholesale.service.WholesaleParsingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/wholesale-uploads")
public class WholesaleUploadController {
	private final WholesaleFileService wholesaleFileService;
	private final WholesaleParsingService wholesaleParsingService;

	public WholesaleUploadController(
			WholesaleFileService wholesaleFileService,
			WholesaleParsingService wholesaleParsingService
	) {
		this.wholesaleFileService = wholesaleFileService;
		this.wholesaleParsingService = wholesaleParsingService;
	}

	@PostMapping("/preview")
	public ApiResponse<WholesaleUploadPreviewResponse> preview(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam MultipartFile file,
			@RequestParam(defaultValue = "AUTO") CsvEncoding encoding,
			@RequestParam(required = false) String sourceName,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				wholesaleFileService.previewUpload(user.userId(), file, encoding, sourceName),
				RequestContext.requestId(request)
		);
	}

	@PostMapping("/{uploadId}/confirm")
	public ApiResponse<WholesaleUploadConfirmResponse> confirm(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long uploadId,
			@Valid @RequestBody WholesaleUploadConfirmRequest requestBody,
			HttpServletRequest request
	) {
		return ApiResponse.success(
				wholesaleParsingService.confirm(user.userId(), uploadId, requestBody),
				RequestContext.requestId(request)
		);
	}
}
