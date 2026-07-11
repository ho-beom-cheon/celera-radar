package com.sellerradar.wholesale.upload;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadAdmissionService {
	private static final int MAX_FILENAME_LENGTH = 255;
	private static final Set<String> CSV_CONTENT_TYPES = Set.of(
			"text/csv",
			"text/plain",
			"application/csv",
			"application/vnd.ms-excel",
			"application/octet-stream"
	);
	private static final Set<String> XLSX_CONTENT_TYPES = Set.of(
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"application/zip",
			"application/octet-stream"
	);

	private final UploadSecurityProperties properties;

	public UploadAdmissionService(UploadSecurityProperties properties) {
		this.properties = properties;
	}

	public AcceptedUpload admit(MultipartFile file, EnumSet<UploadFileType> allowedTypes) {
		if (file == null || file.isEmpty() || file.getSize() <= 0) {
			throw invalid("Upload file is required.");
		}
		validateSize(file.getSize());
		String filename = sanitizeFilename(file.getOriginalFilename());
		UploadFileType fileType = detectFileType(filename, allowedTypes);
		validateContentType(fileType, file.getContentType());

		try {
			byte[] bytes = file.getBytes();
			validateSize(bytes.length);
			validateSignature(fileType, bytes);
			return new AcceptedUpload(filename, fileType.extension(), fileType.name(), bytes);
		} catch (IOException exception) {
			throw invalid("Upload file could not be read.");
		}
	}

	private void validateSize(long size) {
		long maxBytes = properties.maxFileSize().toBytes();
		if (size > maxBytes) {
			throw new BusinessException(
					ErrorCode.CSV_FILE_SIZE_EXCEEDED,
					"Upload file exceeds the security limit of " + maxBytes + " bytes.",
					"file"
			);
		}
	}

	private String sanitizeFilename(String rawFilename) {
		if (rawFilename == null || rawFilename.isBlank()) {
			throw invalid("Upload filename is required.");
		}
		String normalized = rawFilename.replace('\\', '/');
		String filename = normalized.substring(normalized.lastIndexOf('/') + 1).strip();
		if (filename.isBlank() || filename.length() > MAX_FILENAME_LENGTH || filename.indexOf('\0') >= 0) {
			throw invalid("Upload filename is invalid.");
		}
		StringBuilder sanitized = new StringBuilder(filename.length());
		filename.codePoints()
				.filter(codePoint -> !Character.isISOControl(codePoint))
				.forEach(sanitized::appendCodePoint);
		if (sanitized.isEmpty()) {
			throw invalid("Upload filename is invalid.");
		}
		return sanitized.toString();
	}

	private UploadFileType detectFileType(String filename, EnumSet<UploadFileType> allowedTypes) {
		String lowerFilename = filename.toLowerCase(Locale.ROOT);
		return allowedTypes.stream()
				.filter(type -> lowerFilename.endsWith(type.extension()))
				.findFirst()
				.orElseThrow(() -> invalid("Only approved upload extensions are supported."));
	}

	private void validateContentType(UploadFileType fileType, String rawContentType) {
		if (rawContentType == null || rawContentType.isBlank()) {
			throw invalid("Upload Content-Type is required.");
		}
		String contentType = rawContentType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
		Set<String> allowedContentTypes = fileType == UploadFileType.CSV
				? CSV_CONTENT_TYPES
				: XLSX_CONTENT_TYPES;
		if (!allowedContentTypes.contains(contentType)) {
			throw invalid("Upload Content-Type does not match the file extension.");
		}
	}

	private void validateSignature(UploadFileType fileType, byte[] bytes) {
		if (fileType == UploadFileType.XLSX) {
			if (!startsWith(bytes, 0x50, 0x4B, 0x03, 0x04)) {
				throw invalid("XLSX ZIP signature is missing.");
			}
			return;
		}
		if (hasBlockedBinarySignature(bytes) || containsNullByte(bytes)) {
			throw invalid("CSV content has a blocked binary signature.");
		}
	}

	private boolean hasBlockedBinarySignature(byte[] bytes) {
		return startsWith(bytes, 0x4D, 0x5A)
				|| startsWith(bytes, 0x7F, 0x45, 0x4C, 0x46)
				|| startsWith(bytes, 0x25, 0x50, 0x44, 0x46)
				|| startsWith(bytes, 0x50, 0x4B, 0x03, 0x04)
				|| startsWith(bytes, 0xD0, 0xCF, 0x11, 0xE0)
				|| startsWith(bytes, 0x89, 0x50, 0x4E, 0x47)
				|| startsWith(bytes, 0xFF, 0xD8, 0xFF)
				|| startsWith(bytes, "GIF8".getBytes(StandardCharsets.US_ASCII));
	}

	private boolean containsNullByte(byte[] bytes) {
		for (byte value : bytes) {
			if (value == 0) {
				return true;
			}
		}
		return false;
	}

	private boolean startsWith(byte[] bytes, int... signature) {
		if (bytes.length < signature.length) {
			return false;
		}
		for (int index = 0; index < signature.length; index++) {
			if (Byte.toUnsignedInt(bytes[index]) != signature[index]) {
				return false;
			}
		}
		return true;
	}

	private boolean startsWith(byte[] bytes, byte[] signature) {
		if (bytes.length < signature.length) {
			return false;
		}
		for (int index = 0; index < signature.length; index++) {
			if (bytes[index] != signature[index]) {
				return false;
			}
		}
		return true;
	}

	private BusinessException invalid(String message) {
		return new BusinessException(ErrorCode.CSV_INVALID_FORMAT, message, "file");
	}
}
