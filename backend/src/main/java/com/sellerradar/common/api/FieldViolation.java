package com.sellerradar.common.api;

public record FieldViolation(
		String field,
		String message
) {
}
