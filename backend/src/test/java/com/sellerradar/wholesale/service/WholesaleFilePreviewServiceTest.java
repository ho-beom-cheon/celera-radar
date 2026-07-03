package com.sellerradar.wholesale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.wholesale.domain.CsvEncoding;
import com.sellerradar.wholesale.dto.WholesaleFilePreviewResponse;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class WholesaleFilePreviewServiceTest {
	private final CsvEncodingDetector encodingDetector = new CsvEncodingDetector();
	private final WholesaleFilePreviewService previewService = new WholesaleFilePreviewService(
			new WholesaleFileParser(
					encodingDetector,
					new SimpleCsvParser(),
					new XlsxSpreadsheetParser()
			)
	);

	@Test
	void previewCsvReturnsHeaderRowsAndNumericValues() {
		byte[] bytes = """
				productName,supplyPrice,shippingFee
				car brush,"1,200 \uC6D0","\u20A93,000"
				desk tray,2500.50,
				""".getBytes(StandardCharsets.UTF_8);

		WholesaleFilePreviewResponse response = previewService.preview("items.csv", bytes, CsvEncoding.AUTO);

		assertThat(response.fileType()).isEqualTo("CSV");
		assertThat(response.headers()).containsExactly("productName", "supplyPrice", "shippingFee");
		assertThat(response.rowCount()).isEqualTo(2);
		assertThat(response.rows().getFirst().rowNo()).isEqualTo(2);
		assertThat(response.rows().getFirst().cells().get(1).decimalValue()).isEqualByComparingTo("1200");
		assertThat(response.rows().getFirst().cells().get(1).longValue()).isEqualTo(1200L);
		assertThat(response.rows().getFirst().cells().get(2).longValue()).isEqualTo(3000L);
		assertThat(response.rows().get(1).cells().get(1).decimalValue()).isEqualByComparingTo("2500.50");
		assertThat(response.rows().get(1).cells().get(1).longValue()).isNull();
	}

	@Test
	void previewXlsxReturnsFirstSheetRows() throws Exception {
		byte[] bytes = xlsxBytes();

		WholesaleFilePreviewResponse response = previewService.preview("items.xlsx", bytes, CsvEncoding.AUTO);

		assertThat(response.fileType()).isEqualTo("XLSX");
		assertThat(response.headers()).containsExactly("productName", "supplyPrice", "shippingFee");
		assertThat(response.rowCount()).isEqualTo(1);
		assertThat(response.rows().getFirst().cells().getFirst().rawValue()).isEqualTo("desk tray");
		assertThat(response.rows().getFirst().cells().get(1).longValue()).isEqualTo(4500L);
		assertThat(response.rows().getFirst().cells().get(2).longValue()).isEqualTo(2500L);
	}

	@Test
	void previewRejectsUnsupportedExtension() {
		assertThatThrownBy(() -> previewService.preview("items.txt", "a,b".getBytes(StandardCharsets.UTF_8), CsvEncoding.AUTO))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CSV_INVALID_FORMAT);
	}

	@Test
	void previewRejectsEmptyFile() {
		assertThatThrownBy(() -> previewService.preview("items.csv", new byte[0], CsvEncoding.AUTO))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CSV_INVALID_FORMAT);
	}

	@Test
	void previewRejectsMissingHeader() {
		assertThatThrownBy(() -> previewService.preview("items.csv", "\n\n".getBytes(StandardCharsets.UTF_8), CsvEncoding.AUTO))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CSV_INVALID_FORMAT);
	}

	@Test
	void numberParserNormalizesMoneyText() {
		assertThat(WholesaleNumberParser.parseBigDecimal(" 1,234 \uC6D0 ")).isEqualByComparingTo(BigDecimal.valueOf(1234));
		assertThat(WholesaleNumberParser.parseLong("\u20A99,900")).isEqualTo(9900L);
		assertThat(WholesaleNumberParser.parseNonNegativeInteger("-1")).isNull();
		assertThat(WholesaleNumberParser.parsePositiveInteger("0")).isNull();
	}

	private byte[] xlsxBytes() throws Exception {
		try (Workbook workbook = new XSSFWorkbook();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("items");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("productName");
			header.createCell(1).setCellValue("supplyPrice");
			header.createCell(2).setCellValue("shippingFee");
			Row row = sheet.createRow(1);
			row.createCell(0).setCellValue("desk tray");
			row.createCell(1).setCellValue("4,500 \uC6D0");
			row.createCell(2).setCellValue(2500);
			workbook.write(output);
			return output.toByteArray();
		}
	}
}
