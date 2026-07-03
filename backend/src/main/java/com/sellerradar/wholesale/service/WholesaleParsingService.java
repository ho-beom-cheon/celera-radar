package com.sellerradar.wholesale.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.wholesale.domain.WholesaleFile;
import com.sellerradar.wholesale.domain.WholesaleProduct;
import com.sellerradar.wholesale.dto.WholesaleColumnMappingRequest;
import com.sellerradar.wholesale.dto.WholesaleFileResponse;
import com.sellerradar.wholesale.dto.WholesaleParseResponse;
import com.sellerradar.wholesale.dto.WholesaleProductRowResponse;
import com.sellerradar.wholesale.repository.WholesaleFileRepository;
import com.sellerradar.wholesale.repository.WholesaleProductRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WholesaleParsingService {
	private final WholesaleFileRepository wholesaleFileRepository;
	private final WholesaleProductRepository wholesaleProductRepository;
	private final CsvEncodingDetector encodingDetector;
	private final SimpleCsvParser csvParser;
	private final ProductNameNormalizer productNameNormalizer;

	public WholesaleParsingService(
			WholesaleFileRepository wholesaleFileRepository,
			WholesaleProductRepository wholesaleProductRepository,
			CsvEncodingDetector encodingDetector,
			SimpleCsvParser csvParser,
			ProductNameNormalizer productNameNormalizer
	) {
		this.wholesaleFileRepository = wholesaleFileRepository;
		this.wholesaleProductRepository = wholesaleProductRepository;
		this.encodingDetector = encodingDetector;
		this.csvParser = csvParser;
		this.productNameNormalizer = productNameNormalizer;
	}

	@Transactional
	public WholesaleFileResponse updateMapping(
			Long userId,
			Long fileId,
			WholesaleColumnMappingRequest request
	) {
		WholesaleFile file = getFile(userId, fileId);
		CsvDocument document = readDocument(file);
		Map<String, Integer> columnIndex = columnIndex(document.header());
		validateColumn(columnIndex, request.mapping().productName(), "productName");
		validateColumn(columnIndex, request.mapping().supplyPrice(), "supplyPrice");
		validateOptionalColumn(columnIndex, request.mapping().shippingFee(), "shippingFee");
		validateOptionalColumn(columnIndex, request.mapping().category(), "category");
		validateOptionalColumn(columnIndex, request.mapping().productUrl(), "productUrl");
		file.updateMapping(
				request.mapping().productName(),
				request.mapping().supplyPrice(),
				request.mapping().shippingFee(),
				request.mapping().category(),
				request.mapping().productUrl()
		);
		return WholesaleFileResponse.from(file);
	}

	@Transactional
	public WholesaleParseResponse parse(Long userId, Long fileId) {
		WholesaleFile file = getFile(userId, fileId);
		validateMappingExists(file);
		CsvDocument document = readDocument(file);
		Map<String, Integer> columnIndex = columnIndex(document.header());
		WholesaleParseResult result = parseRows(file, document.rows(), columnIndex);
		file.markParsed();
		return new WholesaleParseResponse(file.getId(), result.parsedCount(), result.invalidCount());
	}

	@Transactional(readOnly = true)
	public Page<WholesaleProductRowResponse> rows(Long userId, Long fileId, Pageable pageable) {
		getFile(userId, fileId);
		return wholesaleProductRepository.findByFileIdOrderByRowNoAsc(fileId, pageable)
				.map(WholesaleProductRowResponse::from);
	}

	private WholesaleParseResult parseRows(
			WholesaleFile file,
			List<CsvRow> rows,
			Map<String, Integer> columnIndex
	) {
		wholesaleProductRepository.deleteByFile(file);
		int parsedCount = 0;
		int invalidCount = 0;
		for (CsvRow row : rows) {
			WholesaleProduct product = toProduct(file, row, columnIndex);
			if (product.getParseStatus().name().equals("PARSED")) {
				parsedCount++;
			} else {
				invalidCount++;
			}
			wholesaleProductRepository.save(product);
		}
		return new WholesaleParseResult(parsedCount, invalidCount);
	}

	private WholesaleProduct toProduct(
			WholesaleFile file,
			CsvRow row,
			Map<String, Integer> columnIndex
	) {
		String productName = value(row, columnIndex, file.getMappingProductName());
		String supplyPriceRaw = value(row, columnIndex, file.getMappingSupplyPrice());
		String shippingFeeRaw = value(row, columnIndex, file.getMappingShippingFee());
		String category = value(row, columnIndex, file.getMappingCategory());
		String productUrl = value(row, columnIndex, file.getMappingProductUrl());
		if (productName.isBlank()) {
			return WholesaleProduct.invalid(file, row.rowNo(), "productName is required.");
		}
		Integer supplyPrice = WholesaleNumberParser.parsePositiveInteger(supplyPriceRaw);
		if (supplyPrice == null) {
			return WholesaleProduct.invalid(file, row.rowNo(), "supplyPrice must be a positive number.");
		}
		Integer shippingFee = 0;
		if (!shippingFeeRaw.isBlank()) {
			shippingFee = WholesaleNumberParser.parseNonNegativeInteger(shippingFeeRaw);
			if (shippingFee == null) {
				return WholesaleProduct.invalid(file, row.rowNo(), "shippingFee must be zero or a positive number.");
			}
		}
		return WholesaleProduct.parsed(
				file,
				row.rowNo(),
				productName,
				productNameNormalizer.normalize(productName),
				supplyPrice,
				shippingFee,
				category.isBlank() ? null : category,
				productUrl.isBlank() ? null : productUrl
		);
	}

	private WholesaleFile getFile(Long userId, Long fileId) {
		return wholesaleFileRepository.findByIdAndUserId(fileId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WHOLESALE_FILE_NOT_FOUND));
	}

	private CsvDocument readDocument(WholesaleFile file) {
		try {
			byte[] bytes = Files.readAllBytes(Path.of(file.getStoredPath()));
			return csvParser.parse(encodingDetector.decode(bytes, file.getDetectedEncoding()));
		} catch (IOException exception) {
			file.markFailed();
			throw new BusinessException(ErrorCode.CSV_INVALID_FORMAT, "CSV file could not be read.", "file");
		}
	}

	private Map<String, Integer> columnIndex(List<String> columns) {
		Map<String, Integer> indexes = new HashMap<>();
		for (int i = 0; i < columns.size(); i++) {
			indexes.put(columns.get(i), i);
		}
		return indexes;
	}

	private void validateColumn(Map<String, Integer> columnIndex, String column, String field) {
		if (column == null || column.isBlank() || !columnIndex.containsKey(column)) {
			throw new BusinessException(
					ErrorCode.CSV_REQUIRED_COLUMN_MISSING,
					ErrorCode.CSV_REQUIRED_COLUMN_MISSING.defaultMessage(),
					field
			);
		}
	}

	private void validateOptionalColumn(Map<String, Integer> columnIndex, String column, String field) {
		if (column != null && !column.isBlank() && !columnIndex.containsKey(column)) {
			throw new BusinessException(
					ErrorCode.CSV_REQUIRED_COLUMN_MISSING,
					ErrorCode.CSV_REQUIRED_COLUMN_MISSING.defaultMessage(),
					field
			);
		}
	}

	private void validateMappingExists(WholesaleFile file) {
		if (file.getMappingProductName() == null || file.getMappingSupplyPrice() == null) {
			throw new BusinessException(ErrorCode.CSV_REQUIRED_COLUMN_MISSING);
		}
	}

	private String value(CsvRow row, Map<String, Integer> columnIndex, String column) {
		if (column == null || column.isBlank()) {
			return "";
		}
		Integer index = columnIndex.get(column);
		if (index == null || index >= row.values().size()) {
			return "";
		}
		return row.values().get(index);
	}
}
