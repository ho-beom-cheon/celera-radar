package com.sellerradar.common.external.domain;

import com.sellerradar.keyword.domain.Keyword;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "api_call_log")
public class ApiCallLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ExternalApiProvider provider;

	@Column(name = "api_name", nullable = false, length = 80)
	private String apiName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "keyword_id")
	private Keyword keyword;

	@Column(name = "base_date")
	private LocalDate baseDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApiCallStatus status;

	@Column(name = "http_status")
	private Integer httpStatus;

	@Column(name = "error_code", length = 80)
	private String errorCode;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected ApiCallLog() {
	}

	private ApiCallLog(
			ExternalApiProvider provider,
			String apiName,
			Keyword keyword,
			LocalDate baseDate,
			ApiCallStatus status,
			Integer httpStatus,
			String errorCode,
			String errorMessage
	) {
		this.provider = provider;
		this.apiName = apiName;
		this.keyword = keyword;
		this.baseDate = baseDate;
		this.status = status;
		this.httpStatus = httpStatus;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public static ApiCallLog success(ExternalApiProvider provider, String apiName, Keyword keyword, LocalDate baseDate) {
		return new ApiCallLog(provider, apiName, keyword, baseDate, ApiCallStatus.SUCCESS, 200, null, null);
	}

	public static ApiCallLog failure(
			ExternalApiProvider provider,
			String apiName,
			Keyword keyword,
			LocalDate baseDate,
			Integer httpStatus,
			String errorCode,
			String errorMessage
	) {
		return new ApiCallLog(
				provider,
				apiName,
				keyword,
				baseDate,
				ApiCallStatus.FAILED,
				httpStatus,
				errorCode,
				truncate(errorMessage, 500)
		);
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	private static String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	public Long getId() {
		return id;
	}

	public ExternalApiProvider getProvider() {
		return provider;
	}

	public String getApiName() {
		return apiName;
	}

	public Keyword getKeyword() {
		return keyword;
	}

	public LocalDate getBaseDate() {
		return baseDate;
	}

	public ApiCallStatus getStatus() {
		return status;
	}

	public Integer getHttpStatus() {
		return httpStatus;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
