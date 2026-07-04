package com.sellerradar.wholesale.service;

import com.sellerradar.wholesale.domain.CsvEncoding;

record ParsedWholesaleFile(
		String fileType,
		CsvEncoding detectedEncoding,
		CsvDocument document
) {
}
