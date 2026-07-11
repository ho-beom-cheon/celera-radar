package com.sellerradar.wholesale.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "seller-radar.wholesale.xlsx-security")
public record XlsxSecurityProperties(
		int maxZipEntries,
		DataSize maxUncompressedSize,
		double minInflateRatio,
		int maxSheets,
		int maxRows,
		int maxColumns,
		int maxCellLength,
		DataSize maxExtractedTextSize
) {
}
