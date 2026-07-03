package com.sellerradar.wholesale.service;

import java.util.List;

record CsvDocument(
		List<String> header,
		List<CsvRow> rows
) {
}
