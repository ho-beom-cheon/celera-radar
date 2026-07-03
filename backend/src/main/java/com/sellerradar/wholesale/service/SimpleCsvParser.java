package com.sellerradar.wholesale.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SimpleCsvParser {
	public CsvDocument parse(String content) {
		List<List<String>> records = new ArrayList<>();
		List<String> currentRecord = new ArrayList<>();
		StringBuilder currentValue = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < content.length(); i++) {
			char ch = content.charAt(i);
			if (ch == '"') {
				if (inQuotes && i + 1 < content.length() && content.charAt(i + 1) == '"') {
					currentValue.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
				continue;
			}
			if (ch == ',' && !inQuotes) {
				currentRecord.add(currentValue.toString().strip());
				currentValue.setLength(0);
				continue;
			}
			if ((ch == '\n' || ch == '\r') && !inQuotes) {
				if (ch == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
					i++;
				}
				currentRecord.add(currentValue.toString().strip());
				addIfNotEmpty(records, currentRecord);
				currentRecord = new ArrayList<>();
				currentValue.setLength(0);
				continue;
			}
			currentValue.append(ch);
		}
		currentRecord.add(currentValue.toString().strip());
		addIfNotEmpty(records, currentRecord);

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

	private void addIfNotEmpty(List<List<String>> records, List<String> record) {
		boolean hasValue = record.stream().anyMatch(value -> value != null && !value.isBlank());
		if (hasValue) {
			records.add(record);
		}
	}
}
