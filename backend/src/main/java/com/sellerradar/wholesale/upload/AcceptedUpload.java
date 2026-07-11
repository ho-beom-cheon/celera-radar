package com.sellerradar.wholesale.upload;

public record AcceptedUpload(
		String originalFilename,
		String extension,
		String fileType,
		byte[] bytes
) {
}
