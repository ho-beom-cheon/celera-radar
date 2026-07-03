package com.sellerradar.wholesale.service;

import java.util.List;

record CsvRow(
		int rowNo,
		List<String> values
) {
}
