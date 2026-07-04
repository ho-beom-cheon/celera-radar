package com.sellerradar.wholesale.dto;

import com.sellerradar.wholesale.domain.CsvEncoding;
import com.sellerradar.wholesale.domain.WholesaleFile;
import com.sellerradar.wholesale.domain.WholesaleFileStatus;

public record WholesaleUploadPreviewResponse(
		Long uploadId,
		WholesaleFileStatus status,
		String fileType,
		CsvEncoding detectedEncoding,
		WholesaleFilePreviewResponse preview
) {
	public static WholesaleUploadPreviewResponse from(WholesaleFile upload, WholesaleFilePreviewResponse preview) {
		return new WholesaleUploadPreviewResponse(
				upload.getId(),
				upload.getStatus(),
				upload.getFileType(),
				upload.getDetectedEncoding(),
				preview
		);
	}
}
