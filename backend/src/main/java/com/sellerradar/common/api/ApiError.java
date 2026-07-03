package com.sellerradar.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sellerradar.common.error.ErrorCode;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
		String code,
		String message,
		String field,
		List<FieldViolation> violations
) {
	public static ApiError of(ErrorCode errorCode) {
		return new ApiError(errorCode.name(), errorCode.defaultMessage(), null, null);
	}

	public static ApiError of(ErrorCode errorCode, String message) {
		return new ApiError(errorCode.name(), message, null, null);
	}

	public static ApiError of(ErrorCode errorCode, String message, String field) {
		return new ApiError(errorCode.name(), message, field, null);
	}

	public static ApiError validation(List<FieldViolation> violations) {
		String field = violations.isEmpty() ? null : violations.getFirst().field();
		return new ApiError(
				ErrorCode.VALIDATION_FAILED.name(),
				ErrorCode.VALIDATION_FAILED.defaultMessage(),
				field,
				violations
		);
	}
}
