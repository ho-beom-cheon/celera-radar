package com.sellerradar.common.web;

import com.sellerradar.auth.ratelimit.AuthRateLimitExceededException;
import com.sellerradar.common.api.ApiError;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.api.FieldViolation;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(
			BusinessException exception,
			HttpServletRequest request
	) {
		ErrorCode errorCode = exception.errorCode();
		ApiError error = ApiError.of(errorCode, exception.getMessage(), exception.field());
		return errorResponse(errorCode.status(), error, request);
	}

	@ExceptionHandler(AuthRateLimitExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthRateLimitExceeded(
			AuthRateLimitExceededException exception,
			HttpServletRequest request
	) {
		ApiError error = ApiError.of(exception.errorCode(), exception.getMessage(), exception.field());
		return ResponseEntity
				.status(exception.errorCode().status())
				.header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
				.body(ApiResponse.failure(error, RequestContext.requestId(request)));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		List<FieldViolation> violations = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(this::toFieldViolation)
				.toList();
		return errorResponse(HttpStatus.BAD_REQUEST, ApiError.validation(violations), request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request
	) {
		List<FieldViolation> violations = exception.getConstraintViolations()
				.stream()
				.map(violation -> new FieldViolation(
						violation.getPropertyPath().toString(),
						violation.getMessage()
				))
				.toList();
		return errorResponse(HttpStatus.BAD_REQUEST, ApiError.validation(violations), request);
	}

	@ExceptionHandler({
			HttpMessageNotReadableException.class,
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception exception, HttpServletRequest request) {
		return errorResponse(
				ErrorCode.INVALID_REQUEST.status(),
				ApiError.of(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.defaultMessage()),
				request
		);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
			MaxUploadSizeExceededException exception,
			HttpServletRequest request
	) {
		return errorResponse(
				ErrorCode.CSV_FILE_SIZE_EXCEEDED.status(),
				ApiError.of(ErrorCode.CSV_FILE_SIZE_EXCEEDED, ErrorCode.CSV_FILE_SIZE_EXCEEDED.defaultMessage(), "file"),
				request
		);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
			NoResourceFoundException exception,
			HttpServletRequest request
	) {
		return errorResponse(
				ErrorCode.RESOURCE_NOT_FOUND.status(),
				ApiError.of(ErrorCode.RESOURCE_NOT_FOUND),
				request
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception, HttpServletRequest request) {
		String requestId = RequestContext.requestId(request);
		log.error("Unhandled exception. requestId={}", requestId, exception);
		return errorResponse(
				ErrorCode.INTERNAL_SERVER_ERROR.status(),
				ApiError.of(ErrorCode.INTERNAL_SERVER_ERROR),
				request
		);
	}

	private FieldViolation toFieldViolation(FieldError fieldError) {
		String message = fieldError.getDefaultMessage();
		return new FieldViolation(
				fieldError.getField(),
				message == null ? ErrorCode.VALIDATION_FAILED.defaultMessage() : message
		);
	}

	private ResponseEntity<ApiResponse<Void>> errorResponse(
			HttpStatus status,
			ApiError error,
			HttpServletRequest request
	) {
		String requestId = RequestContext.requestId(request);
		return ResponseEntity
				.status(status)
				.body(ApiResponse.failure(error, requestId));
	}
}
