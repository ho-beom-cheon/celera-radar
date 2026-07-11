package com.sellerradar.wholesale.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class UploadAdmissionServiceTest {
	private final UploadAdmissionService service = new UploadAdmissionService(
			new UploadSecurityProperties("./build/test-quarantine/unit", DataSize.ofBytes(1024))
	);

	@Test
	void acceptsCsvAndRemovesClientPathFromMetadata() {
		AcceptedUpload accepted = service.admit(
				file("C:\\fakepath\\items.csv", "text/csv", "name,price\nitem,1000".getBytes(StandardCharsets.UTF_8)),
				EnumSet.of(UploadFileType.CSV)
		);

		assertThat(accepted.originalFilename()).isEqualTo("items.csv");
		assertThat(accepted.fileType()).isEqualTo("CSV");
	}

	@Test
	void rejectsExecutableSignatureEvenWhenNamedCsv() {
		assertInvalid(file("items.csv", "text/csv", new byte[] {'M', 'Z', 1, 2}));
	}

	@Test
	void rejectsZipSignatureWhenNamedCsv() {
		assertInvalid(file("items.csv", "text/csv", new byte[] {'P', 'K', 3, 4, 1}));
	}

	@Test
	void rejectsXlsxWithoutZipSignature() {
		MockMultipartFile file = file(
				"items.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"not-a-zip".getBytes(StandardCharsets.UTF_8)
		);

		assertThatThrownBy(() -> service.admit(file, EnumSet.of(UploadFileType.CSV, UploadFileType.XLSX)))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.CSV_INVALID_FORMAT));
	}

	@Test
	void rejectsContentTypeThatConflictsWithExtension() {
		assertInvalid(file("items.csv", "application/pdf", "name,price".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void rejectsMissingContentType() {
		assertInvalid(file("items.csv", null, "name,price".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void rejectsSizeBeforeReadingContent() {
		byte[] bytes = new byte[1025];
		MockMultipartFile file = file("items.csv", "text/csv", bytes);

		assertThatThrownBy(() -> service.admit(file, EnumSet.of(UploadFileType.CSV)))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.CSV_FILE_SIZE_EXCEEDED));
	}

	private void assertInvalid(MockMultipartFile file) {
		assertThatThrownBy(() -> service.admit(file, EnumSet.of(UploadFileType.CSV)))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.CSV_INVALID_FORMAT));
	}

	private MockMultipartFile file(String filename, String contentType, byte[] bytes) {
		return new MockMultipartFile("file", filename, contentType, bytes);
	}
}
