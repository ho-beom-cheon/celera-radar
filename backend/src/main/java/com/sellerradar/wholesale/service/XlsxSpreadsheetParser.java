package com.sellerradar.wholesale.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.wholesale.upload.XlsxSecurityProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

@Component
public class XlsxSpreadsheetParser {
	private final XlsxZipGuard zipGuard;
	private final XlsxSecurityProperties properties;

	public XlsxSpreadsheetParser(XlsxZipGuard zipGuard, XlsxSecurityProperties properties) {
		this.zipGuard = zipGuard;
		this.properties = properties;
	}

	public CsvDocument parse(byte[] bytes) {
		zipGuard.validate(bytes);
		try (OPCPackage opcPackage = OPCPackage.open(new ByteArrayInputStream(bytes))) {
			XSSFReader reader = new XSSFReader(opcPackage, true);
			validateSheetCount(reader);
			return parseFirstSheet(opcPackage, reader);
		} catch (BusinessException exception) {
			throw exception;
		} catch (IOException | OpenXML4JException | SAXException | ParserConfigurationException exception) {
			throw invalid("XLSX file could not be read.");
		}
	}

	private void validateSheetCount(XSSFReader reader) throws IOException, OpenXML4JException {
		Iterator<InputStream> sheets = reader.getSheetsData();
		int sheetCount = 0;
		while (sheets.hasNext()) {
			sheetCount++;
			try (InputStream ignored = sheets.next()) {
				if (sheetCount > properties.maxSheets()) {
					throw invalid("XLSX contains too many sheets.");
				}
			}
		}
		if (sheetCount == 0) {
			throw invalid("XLSX must contain at least one sheet.");
		}
	}

	private CsvDocument parseFirstSheet(OPCPackage opcPackage, XSSFReader reader)
			throws IOException, OpenXML4JException, SAXException, ParserConfigurationException {
		StylesTable styles = reader.getStylesTable();
		SharedStrings sharedStrings = new ReadOnlySharedStringsTable(opcPackage);
		SheetHandler sheetHandler = new SheetHandler(properties);
		DataFormatter formatter = new DataFormatter(Locale.KOREA);
		XSSFSheetXMLHandler contentHandler = new XSSFSheetXMLHandler(
				styles,
				sharedStrings,
				sheetHandler,
				formatter,
				false
		);
		XMLReader xmlReader = XMLHelper.newXMLReader();
		xmlReader.setContentHandler(contentHandler);
		Iterator<InputStream> sheets = reader.getSheetsData();
		try (InputStream firstSheet = sheets.next()) {
			xmlReader.parse(new InputSource(firstSheet));
		}
		return sheetHandler.document();
	}

	private BusinessException invalid(String message) {
		return new BusinessException(ErrorCode.CSV_INVALID_FORMAT, message, "file");
	}

	private static final class SheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
		private final XlsxSecurityProperties properties;
		private final List<SheetRow> nonEmptyRows = new ArrayList<>();
		private List<String> currentValues = List.of();
		private int currentRowNumber;
		private long extractedTextBytes;

		private SheetHandler(XlsxSecurityProperties properties) {
			this.properties = properties;
		}

		@Override
		public void startRow(int rowNum) {
			if (rowNum + 1 > properties.maxRows()) {
				throw invalidStatic("XLSX row limit exceeded.");
			}
			currentRowNumber = rowNum + 1;
			currentValues = new ArrayList<>();
		}

		@Override
		public void endRow(int rowNum) {
			List<String> trimmed = trimTrailingBlank(currentValues);
			if (trimmed.stream().anyMatch(value -> !value.isBlank())) {
				nonEmptyRows.add(new SheetRow(currentRowNumber, List.copyOf(trimmed)));
			}
		}

		@Override
		public void cell(String cellReference, String formattedValue, XSSFComment comment) {
			int column = new CellReference(cellReference).getCol();
			if (column + 1 > properties.maxColumns()) {
				throw invalidStatic("XLSX column limit exceeded.");
			}
			String value = formattedValue == null ? "" : formattedValue.strip();
			if (value.length() > properties.maxCellLength()) {
				throw invalidStatic("XLSX cell length limit exceeded.");
			}
			extractedTextBytes += value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
			if (extractedTextBytes > properties.maxExtractedTextSize().toBytes()) {
				throw invalidStatic("XLSX extracted text limit exceeded.");
			}
			while (currentValues.size() < column) {
				currentValues.add("");
			}
			currentValues.add(value);
		}

		private CsvDocument document() {
			if (nonEmptyRows.isEmpty()) {
				return new CsvDocument(List.of(), List.of());
			}
			List<String> header = nonEmptyRows.getFirst().values();
			List<CsvRow> rows = nonEmptyRows.stream()
					.skip(1)
					.map(row -> new CsvRow(row.rowNumber(), row.values()))
					.toList();
			return new CsvDocument(header, rows);
		}

		private List<String> trimTrailingBlank(List<String> values) {
			int lastIndex = values.size() - 1;
			while (lastIndex >= 0 && values.get(lastIndex).isBlank()) {
				lastIndex--;
			}
			return values.subList(0, lastIndex + 1);
		}

		private static BusinessException invalidStatic(String message) {
			return new BusinessException(ErrorCode.CSV_INVALID_FORMAT, message, "file");
		}
	}

	private record SheetRow(int rowNumber, List<String> values) {
	}
}
