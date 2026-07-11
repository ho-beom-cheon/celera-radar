package com.sellerradar.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token이 올바르지 않습니다."),
	AUTH_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "인증 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "키워드를 찾을 수 없습니다."),
	CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "후보를 찾을 수 없습니다."),
	WHOLESALE_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "도매 CSV 파일을 찾을 수 없습니다."),
	WHOLESALE_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "도매 상품을 찾을 수 없습니다."),
	ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
	SMARTSTORE_CONNECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SmartStore connection was not found."),
	SMARTSTORE_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "SmartStore product was not found."),
	STORE_PRODUCT_COST_NOT_FOUND(HttpStatus.NOT_FOUND, "Store product cost mapping was not found."),
	KEYWORD_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "현재 요금제에서 등록 가능한 키워드 수를 초과했습니다."),
	DUPLICATED_KEYWORD(HttpStatus.CONFLICT, "이미 등록된 키워드입니다."),
	CSV_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "CSV 형식이 올바르지 않습니다."),
	CSV_FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "CSV/XLSX 파일 크기가 허용 범위를 초과했습니다."),
	UPLOAD_RAW_DELETE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "업로드 원본 파일을 삭제하지 못했습니다. 잠시 후 다시 시도해 주세요."),
	CSV_REQUIRED_COLUMN_MISSING(HttpStatus.BAD_REQUEST, "CSV 필수 컬럼이 누락되었습니다."),
	CSV_ROW_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "현재 요금제에서 처리 가능한 CSV 행 수를 초과했습니다."),
	EXTERNAL_API_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "외부 API 호출 한도를 초과했습니다."),
	EXTERNAL_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "외부 API를 사용할 수 없습니다."),
	ANALYSIS_NOT_READY(HttpStatus.ACCEPTED, "분석이 아직 완료되지 않았습니다."),
	SUBSCRIPTION_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "유료 기능 이용 권한이 필요합니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String defaultMessage;

	ErrorCode(HttpStatus status, String defaultMessage) {
		this.status = status;
		this.defaultMessage = defaultMessage;
	}

	public HttpStatus status() {
		return status;
	}

	public String defaultMessage() {
		return defaultMessage;
	}
}
