package com.sellerradar.wholesale.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.wholesale.domain.CsvEncoding;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class WholesaleFileParser {
	private final CsvEncodingDetector encodingDetector;
	private final SimpleCsvParser csvParser;
	private final XlsxSpreadsheetParser xlsxParser;

	public WholesaleFileParser(
			CsvEncodingDetector encodingDetector,
			SimpleCsvParser csvParser,
			XlsxSpreadsheetParser xlsxParser
	) {
		this.encodingDetector = encodingDetector;
		this.csvParser = csvParser;
		this.xlsxParser = xlsxParser;
	}

	public ParsedWholesaleFile parse(String originalFilename, byte[] bytes, CsvEncoding requestedEncoding) {
		validateFile(bytes);
		String extension = extension(originalFilename);
		CsvDocument document = switch (extension) {
			case ".csv" -> parseCsv(bytes, requestedEncoding);
			case ".xlsx" -> xlsxParser.parse(bytes);
			default -> throw new BusinessException(
					ErrorCode.CSV_INVALID_FORMAT,
					"Only .csv and .xlsx files are supported.",
					"file"
			);
		};
		validateHeader(document);
		return new ParsedWholesaleFile(fileType(extension), document);
	}

	private CsvDocument parseCsv(byte[] bytes, CsvEncoding requestedEncoding) {
		CsvEncoding encoding = encodingDetector.detect(bytes, requestedEncoding);
		return csvParser.parse(encodingDetector.decode(bytes, encoding));
	}

	private void validateFile(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			throw new BusinessException(ErrorCode.CSV_INVALID_FORMAT, "Upload file is required.", "file");
		}
	}

	private void validateHeader(CsvDocument document) {
		if (document.header().isEmpty() || document.header().stream().noneMatch(column -> !column.isBlank())) {
			throw new BusinessException(ErrorCode.CSV_INVALID_FORMAT, "File header is required.", "file");
		}
	}

	private String extension(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank() || !originalFilename.contains(".")) {
			return "";
		}
		return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
	}

	private String fileType(String extension) {
		return extension.substring(1).toUpperCase(Locale.ROOT);
	}
}
