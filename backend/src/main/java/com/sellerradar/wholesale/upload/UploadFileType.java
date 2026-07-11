package com.sellerradar.wholesale.upload;

public enum UploadFileType {
	CSV(".csv"),
	XLSX(".xlsx");

	private final String extension;

	UploadFileType(String extension) {
		this.extension = extension;
	}

	public String extension() {
		return extension;
	}
}
