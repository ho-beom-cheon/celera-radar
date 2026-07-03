package com.sellerradar.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiMeta(
		String requestId,
		Boolean cached,
		OffsetDateTime generatedAt
) {
	public static ApiMeta of(String requestId) {
		return new ApiMeta(requestId, null, null);
	}

	public static ApiMeta generated(String requestId) {
		return new ApiMeta(requestId, null, OffsetDateTime.now());
	}

	public static ApiMeta cached(String requestId) {
		return new ApiMeta(requestId, true, OffsetDateTime.now());
	}
}
