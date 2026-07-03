package com.sellerradar.alert.dto;

import com.sellerradar.alert.domain.Alert;
import com.sellerradar.alert.domain.AlertStatus;
import com.sellerradar.alert.domain.AlertType;
import java.time.OffsetDateTime;

public record AlertResponse(
		Long id,
		AlertType type,
		AlertStatus status,
		String title,
		String message,
		Long candidateId,
		String candidateName,
		Long ruleId,
		String ruleName,
		OffsetDateTime createdAt,
		OffsetDateTime readAt
) {
	public static AlertResponse from(Alert alert) {
		return new AlertResponse(
				alert.getId(),
				alert.getType(),
				alert.getStatus(),
				alert.getTitle(),
				alert.getMessage(),
				alert.getCandidate().getId(),
				alert.getCandidate().getName(),
				alert.getRule().getId(),
				alert.getRule().getName(),
				alert.getCreatedAt(),
				alert.getReadAt()
		);
	}
}
