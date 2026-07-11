package com.sellerradar.wholesale.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordPriority;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import com.sellerradar.wholesale.domain.WholesaleFileStatus;
import com.sellerradar.wholesale.domain.WholesaleProduct;
import com.sellerradar.wholesale.domain.WholesaleProductParseStatus;
import com.sellerradar.wholesale.dto.WholesaleColumnMappingRequest;
import com.sellerradar.wholesale.dto.WholesaleUploadConfirmRequest;
import com.sellerradar.wholesale.repository.WholesaleFileRepository;
import com.sellerradar.wholesale.repository.WholesaleProductRepository;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
		"seller-radar.wholesale.upload-security.quarantine-directory=./build/test-quarantine/wholesale-controller",
		"seller-radar.wholesale.upload-security.max-file-size=64KB"
})
@AutoConfigureMockMvc
class WholesaleFileControllerIntegrationTest {
	private static final String PASSWORD = "password1234";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private WholesaleProductRepository wholesaleProductRepository;

	@Autowired
	private WholesaleFileRepository wholesaleFileRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private ShoppingPriceSnapshotRepository snapshotRepository;

	@Value("${seller-radar.wholesale.upload-security.quarantine-directory}")
	private String quarantineDirectory;

	@BeforeEach
	void setUp() throws Exception {
		wholesaleProductRepository.deleteAll();
		wholesaleFileRepository.deleteAll();
		clearQuarantine();
	}

	@AfterEach
	void tearDown() throws Exception {
		clearQuarantine();
	}

	@Test
	void uploadCsvStoresMetadataWithoutExposingStoredPath() throws Exception {
		AuthResponse auth = signup("wholesale-upload@example.com");
		MockMultipartFile file = csvFile("items.csv", """
				productName,supplyPrice,shippingFee,category
				car brush,4200,3000,car
				desk tray,6000,2500,office
				""");

		mockMvc.perform(multipart("/api/v1/wholesale-files")
						.file(file)
						.param("encoding", "AUTO")
						.param("sourceName", "sample supplier")
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value(WholesaleFileStatus.UPLOADED.name()))
				.andExpect(jsonPath("$.data.rowCount").value(2))
				.andExpect(jsonPath("$.data.detectedColumns[0]").value("productName"))
				.andExpect(jsonPath("$.data.sourceName").value("sample supplier"))
				.andExpect(jsonPath("$.data.storedPath").doesNotExist());

		assertThat(wholesaleFileRepository.findAll()).hasSize(1);
		Path storedPath = Path.of(wholesaleFileRepository.findAll().getFirst().getStoredPath());
		assertThat(storedPath).exists();
		assertThat(storedPath.getParent()).isEqualTo(quarantineRoot());
		assertThat(storedPath.getFileName().toString())
				.matches("[0-9a-f-]{36}\\.csv")
				.doesNotContain("items");
	}

	@Test
	void uploadRejectsRowsBeyondPlanLimit() throws Exception {
		AuthResponse auth = signup("wholesale-limit@example.com");
		StringBuilder csv = new StringBuilder("productName,supplyPrice\n");
		for (int i = 1; i <= 101; i++) {
			csv.append("item").append(i).append(",1000\n");
		}

		mockMvc.perform(multipart("/api/v1/wholesale-files")
						.file(csvFile("too-many.csv", csv.toString()))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_ROW_LIMIT_EXCEEDED.name()))
				.andExpect(jsonPath("$.error.field").value("file"));
	}

	@Test
	void wholesaleFileDetailAndRowsDoNotExposeOtherUsersFile() throws Exception {
		AuthResponse ownerAuth = signup("wholesale-private-owner@example.com");
		AuthResponse otherAuth = signup("wholesale-private-other@example.com");
		Long fileId = upload(ownerAuth, """
				productName,supplyPrice,shippingFee,category
				car brush,4200,3000,car
				""");
		mapAndParse(ownerAuth, fileId);

		mockMvc.perform(get("/api/v1/wholesale-files/{fileId}", fileId)
						.header("Authorization", bearer(otherAuth)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.WHOLESALE_FILE_NOT_FOUND.name()));

		mockMvc.perform(get("/api/v1/wholesale-files/{fileId}/rows", fileId)
						.header("Authorization", bearer(otherAuth)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.WHOLESALE_FILE_NOT_FOUND.name()));
	}

	@Test
	void previewUploadReturnsUploadIdAndCsvPreview() throws Exception {
		AuthResponse auth = signup("wholesale-preview@example.com");
		MockMultipartFile file = csvFile("items.csv", """
				productName,supplyPrice,shippingFee,imageUrl,category
				car brush,"1,200 원",3000,https://example.com/a.jpg,car
				""");

		mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(file)
						.param("encoding", "AUTO")
						.param("sourceName", "preview supplier")
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.uploadId").isNumber())
				.andExpect(jsonPath("$.data.fileType").value("CSV"))
				.andExpect(jsonPath("$.data.preview.rowCount").value(1))
				.andExpect(jsonPath("$.data.preview.headers[0]").value("productName"))
				.andExpect(jsonPath("$.data.preview.rows[0].cells[1].longValue").value(1200));

		assertThat(wholesaleFileRepository.findAll()).hasSize(1);
	}

	@Test
	void previewUploadSupportsXlsx() throws Exception {
		AuthResponse auth = signup("wholesale-preview-xlsx@example.com");
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"items.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				xlsxBytes()
		);

		mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(file)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.fileType").value("XLSX"))
				.andExpect(jsonPath("$.data.preview.headers[0]").value("productName"))
				.andExpect(jsonPath("$.data.preview.rows[0].cells[1].longValue").value(4500));
	}

	@Test
	void confirmUploadStoresParsedAndInvalidRows() throws Exception {
		AuthResponse auth = signup("wholesale-confirm@example.com");
		Long uploadId = previewUpload(auth, """
				productName,supplyPrice,shippingFee,imageUrl,productUrl,category
				car brush,4200,3000,https://example.com/a.jpg,https://example.com/a,car
				desk tray,not-number,2500,https://example.com/b.jpg,https://example.com/b,office
				""");

		mockMvc.perform(post("/api/v1/wholesale-uploads/{uploadId}/confirm", uploadId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new WholesaleUploadConfirmRequest(
								new WholesaleUploadConfirmRequest.Mapping(
										"productName",
										"supplyPrice",
										"shippingFee",
										"imageUrl",
										"productUrl",
										"category"
								)
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.uploadId").value(uploadId))
				.andExpect(jsonPath("$.data.successCount").value(1))
				.andExpect(jsonPath("$.data.failureCount").value(1))
				.andExpect(jsonPath("$.data.failureReasons[0].rowNo").value(3));

		WholesaleProduct parsedProduct = wholesaleProductRepository.findAll().stream()
				.filter(product -> product.getParseStatus() == WholesaleProductParseStatus.PARSED)
				.findFirst()
				.orElseThrow();
		assertThat(parsedProduct.getImageUrl()).isEqualTo("https://example.com/a.jpg");
		assertThat(parsedProduct.getProductUrl()).isEqualTo("https://example.com/a");
		assertThat(parsedProduct.getSourceCategory()).isEqualTo("car");
		assertThat(wholesaleFileRepository.findById(uploadId).orElseThrow().getStatus())
				.isEqualTo(WholesaleFileStatus.PARSED);
	}

	@Test
	void previewUploadRejectsUnsupportedExtension() throws Exception {
		AuthResponse auth = signup("wholesale-preview-extension@example.com");

		mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(new MockMultipartFile("file", "items.txt", "text/plain", "a,b".getBytes(StandardCharsets.UTF_8)))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_INVALID_FORMAT.name()))
				.andExpect(jsonPath("$.error.field").value("file"));
	}

	@Test
	void previewUploadRejectsFileSizeLimit() throws Exception {
		AuthResponse auth = signup("wholesale-preview-size@example.com");
		byte[] bytes = new byte[64 * 1024 + 1];
		Arrays.fill(bytes, (byte) 'a');

		mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(new MockMultipartFile("file", "items.csv", "text/csv", bytes))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_FILE_SIZE_EXCEEDED.name()))
				.andExpect(jsonPath("$.error.field").value("file"));

		assertRejectedUploadLeavesNoArtifacts();
	}

	@Test
	void legacyUploadRejectsOversizedFileBeforeParsing() throws Exception {
		AuthResponse auth = signup("wholesale-legacy-size@example.com");
		byte[] bytes = new byte[64 * 1024 + 1];
		Arrays.fill(bytes, (byte) 'a');

		mockMvc.perform(multipart("/api/v1/wholesale-files")
						.file(new MockMultipartFile("file", "items.csv", "text/csv", bytes))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_FILE_SIZE_EXCEEDED.name()));

		assertRejectedUploadLeavesNoArtifacts();
	}

	@Test
	void previewUploadRejectsExecutableRenamedAsCsv() throws Exception {
		AuthResponse auth = signup("wholesale-renamed-executable@example.com");
		byte[] executable = new byte[] {'M', 'Z', 0x01, 0x02, 0x03};

		mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(new MockMultipartFile("file", "report.csv", "text/csv", executable))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_INVALID_FORMAT.name()));

		assertRejectedUploadLeavesNoArtifacts();
	}

	@Test
	void previewUploadRejectsXlsxRenamedAsCsv() throws Exception {
		AuthResponse auth = signup("wholesale-xlsx-renamed-csv@example.com");

		mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(new MockMultipartFile("file", "report.csv", "text/csv", xlsxBytes()))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_INVALID_FORMAT.name()));

		assertRejectedUploadLeavesNoArtifacts();
	}

	@Test
	void previewUploadRejectsCsvRenamedAsXlsx() throws Exception {
		AuthResponse auth = signup("wholesale-csv-renamed-xlsx@example.com");

		mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(new MockMultipartFile(
								"file",
								"report.xlsx",
								"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
								"productName,supplyPrice\nitem,1000".getBytes(StandardCharsets.UTF_8)
						))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_INVALID_FORMAT.name()));

		assertRejectedUploadLeavesNoArtifacts();
	}

	@Test
	void previewUploadRejectsZeroByteFile() throws Exception {
		AuthResponse auth = signup("wholesale-empty-upload@example.com");

		mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_INVALID_FORMAT.name()));

		assertRejectedUploadLeavesNoArtifacts();
	}

	@Test
	void previewUploadRemovesQuarantineObjectWhenParsingFails() throws Exception {
		AuthResponse auth = signup("wholesale-invalid-header@example.com");

		mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(new MockMultipartFile(
								"file",
								"invalid.csv",
								"text/csv",
								"   \n".getBytes(StandardCharsets.UTF_8)
						))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_INVALID_FORMAT.name()));

		assertRejectedUploadLeavesNoArtifacts();
	}

	@Test
	void previewUploadRejectsContentTypeThatConflictsWithExtension() throws Exception {
		AuthResponse auth = signup("wholesale-content-type@example.com");

		mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(new MockMultipartFile(
								"file",
								"items.csv",
								"application/pdf",
								"productName,supplyPrice\nitem,1000".getBytes(StandardCharsets.UTF_8)
						))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_INVALID_FORMAT.name()));

		assertRejectedUploadLeavesNoArtifacts();
	}

	@Test
	void confirmUploadRejectsMissingMappedColumn() throws Exception {
		AuthResponse auth = signup("wholesale-confirm-mapping-error@example.com");
		Long uploadId = previewUpload(auth, """
				productName,supplyPrice
				car brush,4200
				""");

		mockMvc.perform(post("/api/v1/wholesale-uploads/{uploadId}/confirm", uploadId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new WholesaleUploadConfirmRequest(
								new WholesaleUploadConfirmRequest.Mapping(
										"missingName",
										"supplyPrice",
										null,
										null,
										null,
										null
								)
						))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_REQUIRED_COLUMN_MISSING.name()))
				.andExpect(jsonPath("$.error.field").value("productName"));
	}

	@Test
	void mappingAndParseStoreParsedAndInvalidRows() throws Exception {
		AuthResponse auth = signup("wholesale-parse@example.com");
		Long fileId = upload(auth, """
				productName,supplyPrice,shippingFee,category,productUrl
				car brush,4200,3000,car,https://example.com/a
				desk tray,not-number,2500,office,https://example.com/b
				,1000,0,storage,https://example.com/c
				""");

		mockMvc.perform(post("/api/v1/wholesale-files/{fileId}/column-mapping", fileId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new WholesaleColumnMappingRequest(
								new WholesaleColumnMappingRequest.Mapping(
										"productName",
										"supplyPrice",
										"shippingFee",
										"category",
										"productUrl"
								)
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(WholesaleFileStatus.MAPPED.name()));

		mockMvc.perform(post("/api/v1/wholesale-files/{fileId}/parse", fileId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.parsedCount").value(1))
				.andExpect(jsonPath("$.data.invalidCount").value(2));

		mockMvc.perform(get("/api/v1/wholesale-files/{fileId}/rows", fileId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(3))
				.andExpect(jsonPath("$.data.items[0].parseStatus").value(WholesaleProductParseStatus.PARSED.name()))
				.andExpect(jsonPath("$.data.items[0].supplyPrice").value(4200))
				.andExpect(jsonPath("$.data.items[1].parseStatus").value(WholesaleProductParseStatus.INVALID.name()))
				.andExpect(jsonPath("$.data.items[1].errorMessage").value("supplyPrice must be a positive number."))
				.andExpect(jsonPath("$.data.items[2].parseStatus").value(WholesaleProductParseStatus.INVALID.name()));

		WholesaleProduct parsedProduct = wholesaleProductRepository.findAll().stream()
				.filter(product -> product.getParseStatus() == WholesaleProductParseStatus.PARSED)
				.findFirst()
				.orElseThrow();
		assertThat(parsedProduct.getUser().getId()).isEqualTo(auth.userId());
		assertThat(parsedProduct.getSourceName()).isNull();
		assertThat(parsedProduct.getSourceCategory()).isEqualTo("car");
		assertThat(parsedProduct.isSoldOut()).isFalse();
	}

	@Test
	void mappingRejectsMissingRequiredColumn() throws Exception {
		AuthResponse auth = signup("wholesale-mapping-error@example.com");
		Long fileId = upload(auth, """
				productName,supplyPrice
				car brush,4200
				""");

		mockMvc.perform(post("/api/v1/wholesale-files/{fileId}/column-mapping", fileId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new WholesaleColumnMappingRequest(
								new WholesaleColumnMappingRequest.Mapping(
										"missingName",
										"supplyPrice",
										null,
										null,
										null
								)
						))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CSV_REQUIRED_COLUMN_MISSING.name()))
				.andExpect(jsonPath("$.error.field").value("productName"));
	}

	@Test
	void generateCandidatesCreatesCsvCandidateAndSkipsDuplicateRows() throws Exception {
		AuthResponse auth = signup("wholesale-candidate@example.com");
		User user = userRepository.findById(auth.userId()).orElseThrow();
		Keyword keyword = keywordRepository.save(Keyword.create(
				user,
				"car brush",
				"car brush",
				CategoryCode.CAR_ACCESSORY,
				KeywordPriority.MEDIUM
		));
		snapshotRepository.save(ShoppingPriceSnapshot.create(
				keyword,
				LocalDate.of(2026, 7, 2),
				1200L,
				9900,
				15900,
				12900,
				"{}"
		));
		Long fileId = upload(auth, """
				productName,supplyPrice,shippingFee,category
				car brush set,4200,3000,car
				""");
		mapAndParse(auth, fileId);

		mockMvc.perform(post("/api/v1/wholesale-files/{fileId}/candidates", fileId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.generatedCount").value(1))
				.andExpect(jsonPath("$.data.skippedCount").value(0));

		mockMvc.perform(post("/api/v1/wholesale-files/{fileId}/candidates", fileId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.generatedCount").value(0))
				.andExpect(jsonPath("$.data.skippedCount").value(1));

		MvcResult listResult = mockMvc.perform(get("/api/v1/candidates")
						.header("Authorization", bearer(auth))
						.param("source", "CSV"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].name").value("car brush set"))
				.andExpect(jsonPath("$.data.items[0].categoryCode").value(CategoryCode.CAR_ACCESSORY.name()))
				.andExpect(jsonPath("$.data.items[0].score").value(70))
				.andExpect(jsonPath("$.data.items[0].grade").value("REVIEW"))
				.andExpect(jsonPath("$.data.items[0].expectedSalePrice").value(12900))
				.andExpect(jsonPath("$.data.items[0].expectedMarginRate").value(44.19))
				.andReturn();
		Long candidateId = objectMapper.readTree(listResult.getResponse().getContentAsByteArray())
				.get("data")
				.get("items")
				.get(0)
				.get("candidateId")
				.asLong();

		mockMvc.perform(get("/api/v1/candidates/{candidateId}", candidateId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.scoreBreakdown.trendScore").value(0))
				.andExpect(jsonPath("$.data.scoreBreakdown.competitionScore").value(25))
				.andExpect(jsonPath("$.data.scoreBreakdown.marginScore").value(30))
				.andExpect(jsonPath("$.data.scoreBreakdown.priceBandScore").value(10))
				.andExpect(jsonPath("$.data.scoreBreakdown.priceScore").value(10))
				.andExpect(jsonPath("$.data.scoreBreakdown.supplyScore").value(5))
				.andExpect(jsonPath("$.data.scoreBreakdown.riskPenalty").value(0))
				.andExpect(jsonPath("$.data.warnings[0]").value("데이터 기반 검토 후보이며 판매나 수익을 보장하지 않습니다."));
	}

	private Long upload(AuthResponse auth, String content) throws Exception {
		MvcResult result = mockMvc.perform(multipart("/api/v1/wholesale-files")
						.file(csvFile("items.csv", content))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsByteArray())
				.get("data")
				.get("fileId")
				.asLong();
	}

	private Long previewUpload(AuthResponse auth, String content) throws Exception {
		MvcResult result = mockMvc.perform(multipart("/api/v1/wholesale-uploads/preview")
						.file(csvFile("items.csv", content))
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsByteArray())
				.get("data")
				.get("uploadId")
				.asLong();
	}

	private void mapAndParse(AuthResponse auth, Long fileId) throws Exception {
		mockMvc.perform(post("/api/v1/wholesale-files/{fileId}/column-mapping", fileId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new WholesaleColumnMappingRequest(
								new WholesaleColumnMappingRequest.Mapping(
										"productName",
										"supplyPrice",
										"shippingFee",
										"category",
										null
								)
						))))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/wholesale-files/{fileId}/parse", fileId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk());
	}

	private MockMultipartFile csvFile(String filename, String content) {
		return new MockMultipartFile(
				"file",
				filename,
				"text/csv",
				content.getBytes(StandardCharsets.UTF_8)
		);
	}

	private byte[] xlsxBytes() throws Exception {
		try (Workbook workbook = new XSSFWorkbook();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("items");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("productName");
			header.createCell(1).setCellValue("supplyPrice");
			Row row = sheet.createRow(1);
			row.createCell(0).setCellValue("desk tray");
			row.createCell(1).setCellValue("4,500 \uC6D0");
			workbook.write(output);
			return output.toByteArray();
		}
	}

	private void assertRejectedUploadLeavesNoArtifacts() throws Exception {
		assertThat(wholesaleFileRepository.count()).isZero();
		assertThat(quarantineFiles()).isEmpty();
	}

	private List<Path> quarantineFiles() throws Exception {
		Path root = quarantineRoot();
		if (Files.notExists(root)) {
			return List.of();
		}
		try (var paths = Files.list(root)) {
			return paths.filter(Files::isRegularFile).toList();
		}
	}

	private void clearQuarantine() throws Exception {
		Path root = quarantineRoot();
		if (Files.notExists(root)) {
			return;
		}
		try (var paths = Files.walk(root)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}

	private Path quarantineRoot() {
		return Path.of(quarantineDirectory).toAbsolutePath().normalize();
	}

	private AuthResponse signup(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SignupRequest(email, PASSWORD, true))))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode data = objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
		return new AuthResponse(
				data.get("userId").asLong(),
				data.get("email").asText(),
				null,
				data.get("accessToken").asText(),
				data.get("refreshToken").asText()
		);
	}

	private String bearer(AuthResponse auth) {
		return "Bearer " + auth.accessToken();
	}
}
