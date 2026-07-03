package com.sellerradar.common.api;

public record ApiResponse<T>(
		boolean success,
		T data,
		ApiError error,
		ApiMeta meta
) {
	public static <T> ApiResponse<T> success(T data, String requestId) {
		return new ApiResponse<>(true, data, null, ApiMeta.generated(requestId));
	}

	public static ApiResponse<Void> failure(ApiError error, String requestId) {
		return new ApiResponse<>(false, null, error, ApiMeta.of(requestId));
	}
}
