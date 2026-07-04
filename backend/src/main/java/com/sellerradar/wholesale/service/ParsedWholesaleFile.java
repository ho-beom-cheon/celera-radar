package com.sellerradar.wholesale.service;

record ParsedWholesaleFile(
		String fileType,
		CsvDocument document
) {
}
