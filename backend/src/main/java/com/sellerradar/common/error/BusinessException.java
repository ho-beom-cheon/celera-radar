package com.sellerradar.common.error;

public class BusinessException extends RuntimeException {
	private final ErrorCode errorCode;
	private final String field;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.defaultMessage(), null);
	}

	public BusinessException(ErrorCode errorCode, String message) {
		this(errorCode, message, null);
	}

	public BusinessException(ErrorCode errorCode, String message, String field) {
		super(message);
		this.errorCode = errorCode;
		this.field = field;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}

	public String field() {
		return field;
	}
}
