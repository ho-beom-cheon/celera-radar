package com.sellerradar.wholesale.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import com.sellerradar.wholesale.domain.CsvEncoding;
import com.sellerradar.wholesale.domain.WholesaleFile;
import com.sellerradar.wholesale.dto.WholesaleFileResponse;
import com.sellerradar.wholesale.repository.WholesaleFileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WholesaleFileService {
	private final WholesaleFileRepository wholesaleFileRepository;
	private final UserRepository userRepository;
	private final CsvEncodingDetector encodingDetector;
	private final SimpleCsvParser csvParser;
	private final Path uploadRoot;

	public WholesaleFileService(
			WholesaleFileRepository wholesaleFileRepository,
			UserRepository userRepository,
			CsvEncodingDetector encodingDetector,
			SimpleCsvParser csvParser,
			@Value("${seller-radar.upload-dir:uploads}") String uploadDir
	) {
		this.wholesaleFileRepository = wholesaleFileRepository;
		this.userRepository = userRepository;
		this.encodingDetector = encodingDetector;
		this.csvParser = csvParser;
		this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
	}

	@Transactional
	public WholesaleFileResponse upload(
			Long userId,
			MultipartFile file,
			CsvEncoding requestedEncoding,
			String sourceName
	) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.CSV_INVALID_FORMAT, "CSV file is required.", "file");
		}
		String originalFilename = sanitizeFilename(file.getOriginalFilename());
		if (!originalFilename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
			throw new BusinessException(ErrorCode.CSV_INVALID_FORMAT, "Only .csv files are supported.", "file");
		}
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		try {
			byte[] bytes = file.getBytes();
			CsvEncoding encoding = encodingDetector.detect(bytes, requestedEncoding);
			CsvDocument document = csvParser.parse(encodingDetector.decode(bytes, encoding));
			if (document.header().isEmpty()) {
				throw new BusinessException(ErrorCode.CSV_INVALID_FORMAT, "CSV header is required.", "file");
			}
			int rowCount = document.rows().size();
			if (rowCount > user.getPlanCode().csvRowLimit()) {
				throw new BusinessException(
						ErrorCode.CSV_ROW_LIMIT_EXCEEDED,
						"CSV row count exceeds current plan limit.",
						"file"
				);
			}
			Path storedPath = store(bytes, originalFilename);
			WholesaleFile uploaded = WholesaleFile.uploaded(
					user,
					sourceName,
					originalFilename,
					storedPath.toString(),
					bytes.length,
					requestedEncoding,
					encoding,
					rowCount,
					document.header()
			);
			return WholesaleFileResponse.from(wholesaleFileRepository.save(uploaded));
		} catch (IOException exception) {
			throw new BusinessException(ErrorCode.CSV_INVALID_FORMAT, "CSV file could not be read.", "file");
		}
	}

	@Transactional(readOnly = true)
	public WholesaleFileResponse get(Long userId, Long fileId) {
		return WholesaleFileResponse.from(getFile(userId, fileId));
	}

	@Transactional(readOnly = true)
	public WholesaleFile getFile(Long userId, Long fileId) {
		return wholesaleFileRepository.findByIdAndUserId(fileId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WHOLESALE_FILE_NOT_FOUND));
	}

	private Path store(byte[] bytes, String originalFilename) throws IOException {
		Files.createDirectories(uploadRoot);
		String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
		Path storedPath = uploadRoot.resolve(UUID.randomUUID() + extension).normalize();
		if (!storedPath.startsWith(uploadRoot)) {
			throw new IOException("Invalid upload path");
		}
		Files.write(storedPath, bytes);
		return storedPath;
	}

	private String sanitizeFilename(String filename) {
		if (filename == null || filename.isBlank()) {
			return "upload.csv";
		}
		return Path.of(filename).getFileName().toString();
	}
}
