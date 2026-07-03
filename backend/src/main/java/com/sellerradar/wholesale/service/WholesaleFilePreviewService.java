package com.sellerradar.wholesale.service;

import com.sellerradar.wholesale.domain.CsvEncoding;
import com.sellerradar.wholesale.dto.WholesaleFilePreviewCellResponse;
import com.sellerradar.wholesale.dto.WholesaleFilePreviewResponse;
import com.sellerradar.wholesale.dto.WholesaleFilePreviewRowResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WholesaleFilePreviewService {
	private final WholesaleFileParser fileParser;

	public WholesaleFilePreviewService(WholesaleFileParser fileParser) {
		this.fileParser = fileParser;
	}

	public WholesaleFilePreviewResponse preview(
			String originalFilename,
			byte[] bytes,
			CsvEncoding requestedEncoding
	) {
		ParsedWholesaleFile parsedFile = fileParser.parse(originalFilename, bytes, requestedEncoding);
		CsvDocument document = parsedFile.document();
		List<WholesaleFilePreviewRowResponse> rows = document.rows().stream()
				.map(row -> toRow(document.header(), row))
				.toList();
		return new WholesaleFilePreviewResponse(
				originalFilename,
				parsedFile.fileType(),
				document.header(),
				rows,
				document.rows().size()
		);
	}

	private WholesaleFilePreviewRowResponse toRow(List<String> headers, CsvRow row) {
		int cellCount = Math.max(headers.size(), row.values().size());
		List<WholesaleFilePreviewCellResponse> cells = new ArrayList<>(cellCount);
		for (int i = 0; i < cellCount; i++) {
			String header = i < headers.size() ? headers.get(i) : "column" + (i + 1);
			String rawValue = i < row.values().size() ? row.values().get(i) : "";
			BigDecimal decimalValue = WholesaleNumberParser.parseBigDecimal(rawValue);
			Long longValue = WholesaleNumberParser.parseLong(rawValue);
			cells.add(new WholesaleFilePreviewCellResponse(header, rawValue, decimalValue, longValue));
		}
		return new WholesaleFilePreviewRowResponse(row.rowNo(), cells);
	}
}
