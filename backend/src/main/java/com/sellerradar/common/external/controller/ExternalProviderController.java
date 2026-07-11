package com.sellerradar.common.external.controller;

import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.external.dto.ExternalProviderCapabilitiesResponse;
import com.sellerradar.common.external.provider.NaverProviderCapabilities;
import com.sellerradar.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/external/providers")
public class ExternalProviderController {
	private final NaverProviderCapabilities naverProviderCapabilities;

	public ExternalProviderController(NaverProviderCapabilities naverProviderCapabilities) {
		this.naverProviderCapabilities = naverProviderCapabilities;
	}

	@GetMapping("/naver/capabilities")
	public ApiResponse<ExternalProviderCapabilitiesResponse> naverCapabilities(HttpServletRequest request) {
		return ApiResponse.success(
				ExternalProviderCapabilitiesResponse.naver(naverProviderCapabilities.descriptor()),
				RequestContext.requestId(request)
		);
	}
}
