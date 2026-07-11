package com.sellerradar.wholesale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.wholesale.upload.XlsxSecurityProperties;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class XlsxSpreadsheetParserSecurityTest {
	@Test
	void rejectsHighlyCompressedZipEntry() throws Exception {
		XlsxSecurityProperties properties = properties(10, 100, 10000, 0.5);
		XlsxZipGuard guard = new XlsxZipGuard(properties);
		byte[] bytes;
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(output)) {
			zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
			zip.write(new byte[100_000]);
			zip.closeEntry();
			zip.finish();
			bytes = output.toByteArray();
		}

		assertInvalid(() -> guard.validate(bytes));
	}

	@Test
	void rejectsSheetLimit() throws Exception {
		byte[] bytes = workbook(workbook -> {
			workbook.createSheet("first").createRow(0).createCell(0).setCellValue("header");
			workbook.createSheet("second").createRow(0).createCell(0).setCellValue("header");
		});

		assertInvalid(() -> parser(properties(1, 100, 10000, 0.01)).parse(bytes));
	}

	@Test
	void rejectsSparseRowBeyondLimit() throws Exception {
		byte[] bytes = workbook(workbook -> {
			var sheet = workbook.createSheet("items");
			sheet.createRow(0).createCell(0).setCellValue("header");
			sheet.createRow(2).createCell(0).setCellValue("value");
		});

		assertInvalid(() -> parser(properties(10, 2, 10000, 0.01)).parse(bytes));
	}

	@Test
	void rejectsColumnBeyondLimit() throws Exception {
		byte[] bytes = workbook(workbook -> {
			var row = workbook.createSheet("items").createRow(0);
			row.createCell(2).setCellValue("third-column");
		});

		assertInvalid(() -> parser(properties(10, 100, 2, 0.01)).parse(bytes));
	}

	@Test
	void rejectsOversizedCell() throws Exception {
		byte[] bytes = workbook(workbook ->
				workbook.createSheet("items").createRow(0).createCell(0).setCellValue("too-long"));

		XlsxSecurityProperties properties = new XlsxSecurityProperties(
				1000, DataSize.ofMegabytes(50), 0.01, 10, 100, 100, 3, DataSize.ofMegabytes(20)
		);
		assertInvalid(() -> parser(properties).parse(bytes));
	}

	@Test
	void doesNotEvaluateFormulaWithoutCachedValue() throws Exception {
		byte[] bytes = workbook(workbook -> {
			var sheet = workbook.createSheet("items");
			var header = sheet.createRow(0);
			header.createCell(0).setCellValue("name");
			header.createCell(1).setCellValue("formula");
			var row = sheet.createRow(1);
			row.createCell(0).setCellValue("item");
			row.createCell(1).setCellFormula("1+1");
		});

		CsvDocument document = parser(properties(10, 100, 10000, 0.01)).parse(bytes);

		assertThat(document.rows()).hasSize(1);
		assertThat(document.rows().getFirst().values()).containsExactly("item");
	}

	private XlsxSpreadsheetParser parser(XlsxSecurityProperties properties) {
		return new XlsxSpreadsheetParser(new XlsxZipGuard(properties), properties);
	}

	private XlsxSecurityProperties properties(int maxSheets, int maxRows, int maxColumns, double ratio) {
		return new XlsxSecurityProperties(
				1000,
				DataSize.ofMegabytes(50),
				ratio,
				maxSheets,
				maxRows,
				maxColumns,
				10000,
				DataSize.ofMegabytes(20)
		);
	}

	private byte[] workbook(Consumer<Workbook> setup) throws Exception {
		try (Workbook workbook = new XSSFWorkbook();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			setup.accept(workbook);
			workbook.write(output);
			return output.toByteArray();
		}
	}

	private void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
		assertThatThrownBy(callable)
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.CSV_INVALID_FORMAT));
	}
}
