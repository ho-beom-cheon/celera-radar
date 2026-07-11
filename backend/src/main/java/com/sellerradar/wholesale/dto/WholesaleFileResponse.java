package com.sellerradar.wholesale.dto;

import com.sellerradar.wholesale.domain.CsvEncoding;
import com.sellerradar.wholesale.domain.WholesaleFile;
import com.sellerradar.wholesale.domain.WholesaleFileStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record WholesaleFileResponse(
		Long fileId,
		WholesaleFileStatus status,
		int rowCount,
		List<String> detectedColumns,
		String sourceName,
		String originalFilename,
		CsvEncoding detectedEncoding,
		OffsetDateTime createdAt,
		OffsetDateTime rawExpiresAt,
		OffsetDateTime rawDeletedAt
) {
	public static WholesaleFileResponse from(WholesaleFile file) {
		return new WholesaleFileResponse(
				file.getId(),
				file.getStatus(),
				file.getRowCount(),
				file.detectedColumns(),
				file.getSourceName(),
				file.getOriginalFilename(),
				file.getDetectedEncoding(),
				file.getCreatedAt(),
				file.getRawExpiresAt(),
				file.getRawDeletedAt()
		);
	}
}
