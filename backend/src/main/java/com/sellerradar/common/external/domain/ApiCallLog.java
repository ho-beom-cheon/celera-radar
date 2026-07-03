package com.sellerradar.common.external.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "api_call_logs")
public class ApiCallLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ExternalApiProvider provider;

	@Column(nullable = false, length = 200)
	private String endpoint;

	@Column(name = "request_key", length = 500)
	private String requestKey;

	@Column(nullable = false)
	private boolean success;

	@Column(name = "http_status")
	private Integer httpStatus;

	@Column(name = "duration_ms")
	private Integer durationMs;

	@Column(name = "error_code", length = 100)
	private String errorCode;

	@Column(name = "error_message")
	private String errorMessage;

	@Column(name = "rate_limit_scope", length = 100)
	private String rateLimitScope;

	@Column(name = "called_at", nullable = false, updatable = false)
	private OffsetDateTime calledAt;

	protected ApiCallLog() {
	}

	private ApiCallLog(
			ExternalApiProvider provider,
			String endpoint,
			String requestKey,
			boolean success,
			Integer httpStatus,
			Integer durationMs,
			String errorCode,
			String errorMessage,
			String rateLimitScope
	) {
		this.provider = provider;
		this.endpoint = endpoint;
		this.requestKey = requestKey;
		this.success = success;
		this.httpStatus = httpStatus;
		this.durationMs = durationMs;
		this.errorCode = errorCode;
		this.errorMessage = truncate(errorMessage, 1000);
		this.rateLimitScope = rateLimitScope;
	}

	public static ApiCallLog success(ExternalApiProvider provider, String endpoint, Long keywordId, LocalDate baseDate) {
		return new ApiCallLog(provider, endpoint, requestKey(endpoint, keywordId, baseDate), true, 200, null, null, null, null);
	}

	public static ApiCallLog failure(
			ExternalApiProvider provider,
			String endpoint,
			Long keywordId,
			LocalDate baseDate,
			Integer httpStatus,
			String errorCode,
			String errorMessage
	) {
		return new ApiCallLog(
				provider,
				endpoint,
				requestKey(endpoint, keywordId, baseDate),
				false,
				httpStatus,
				null,
				errorCode,
				errorMessage,
				null
		);
	}

	@PrePersist
	void onCreate() {
		this.calledAt = OffsetDateTime.now();
	}

	private static String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	private static String requestKey(String endpoint, Long keywordId, LocalDate baseDate) {
		if (keywordId == null && baseDate == null) {
			return endpoint;
		}
		return endpoint + ":keyword=" + keywordId + ":date=" + baseDate;
	}

	public Long getId() {
		return id;
	}

	public ExternalApiProvider getProvider() {
		return provider;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getApiName() {
		return endpoint;
	}

	public String getRequestKey() {
		return requestKey;
	}

	public LocalDate getBaseDate() {
		return null;
	}

	public boolean isSuccess() {
		return success;
	}

	public ApiCallStatus getStatus() {
		return success ? ApiCallStatus.SUCCESS : ApiCallStatus.FAILED;
	}

	public Integer getHttpStatus() {
		return httpStatus;
	}

	public Integer getDurationMs() {
		return durationMs;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getRateLimitScope() {
		return rateLimitScope;
	}

	public OffsetDateTime getCalledAt() {
		return calledAt;
	}

	public OffsetDateTime getCreatedAt() {
		return calledAt;
	}
}
