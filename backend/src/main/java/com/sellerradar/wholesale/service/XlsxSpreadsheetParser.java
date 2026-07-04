package com.sellerradar.wholesale.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class XlsxSpreadsheetParser {
	public CsvDocument parse(byte[] bytes) {
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
			if (workbook.getNumberOfSheets() == 0) {
				return new CsvDocument(List.of(), List.of());
			}
			return parseSheet(workbook.getSheetAt(0));
		} catch (IOException | RuntimeException exception) {
			throw new BusinessException(ErrorCode.CSV_INVALID_FORMAT, "XLSX file could not be read.", "file");
		}
	}

	private CsvDocument parseSheet(Sheet sheet) {
		DataFormatter formatter = new DataFormatter(Locale.KOREA);
		List<List<String>> records = new ArrayList<>();
		for (int rowNo = sheet.getFirstRowNum(); rowNo <= sheet.getLastRowNum(); rowNo++) {
			Row row = sheet.getRow(rowNo);
			List<String> values = values(row, formatter);
			if (values.stream().anyMatch(value -> !value.isBlank())) {
				records.add(values);
			}
		}
		if (records.isEmpty()) {
			return new CsvDocument(List.of(), List.of());
		}
		List<String> header = records.getFirst();
		List<CsvRow> rows = new ArrayList<>();
		for (int i = 1; i < records.size(); i++) {
			rows.add(new CsvRow(i + 1, records.get(i)));
		}
		return new CsvDocument(header, rows);
	}

	private List<String> values(Row row, DataFormatter formatter) {
		if (row == null || row.getLastCellNum() < 0) {
			return List.of();
		}
		List<String> values = new ArrayList<>(row.getLastCellNum());
		for (int cellNo = 0; cellNo < row.getLastCellNum(); cellNo++) {
			values.add(formatter.formatCellValue(row.getCell(cellNo)).strip());
		}
		return trimTrailingBlank(values);
	}

	private List<String> trimTrailingBlank(List<String> values) {
		int lastIndex = values.size() - 1;
		while (lastIndex >= 0 && values.get(lastIndex).isBlank()) {
			lastIndex--;
		}
		return values.subList(0, lastIndex + 1);
	}
}
