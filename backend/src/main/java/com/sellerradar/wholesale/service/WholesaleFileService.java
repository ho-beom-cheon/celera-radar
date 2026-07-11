package com.sellerradar.wholesale.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import com.sellerradar.wholesale.domain.CsvEncoding;
import com.sellerradar.wholesale.domain.WholesaleFile;
import com.sellerradar.wholesale.dto.WholesaleFileResponse;
import com.sellerradar.wholesale.dto.WholesaleFilePreviewResponse;
import com.sellerradar.wholesale.dto.WholesaleUploadPreviewResponse;
import com.sellerradar.wholesale.repository.WholesaleFileRepository;
import com.sellerradar.wholesale.upload.AcceptedUpload;
import com.sellerradar.wholesale.upload.UploadAdmissionService;
import com.sellerradar.wholesale.upload.UploadFileType;
import com.sellerradar.wholesale.upload.UploadQuarantineStorage;
import java.nio.file.Path;
import java.util.EnumSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WholesaleFileService {
	private final WholesaleFileRepository wholesaleFileRepository;
	private final UserRepository userRepository;
	private final CsvEncodingDetector encodingDetector;
	private final SimpleCsvParser csvParser;
	private final WholesaleFileParser fileParser;
	private final WholesaleFilePreviewService previewService;
	private final UploadAdmissionService uploadAdmissionService;
	private final UploadQuarantineStorage quarantineStorage;

	public WholesaleFileService(
			WholesaleFileRepository wholesaleFileRepository,
			UserRepository userRepository,
			CsvEncodingDetector encodingDetector,
			SimpleCsvParser csvParser,
			WholesaleFileParser fileParser,
			WholesaleFilePreviewService previewService,
			UploadAdmissionService uploadAdmissionService,
			UploadQuarantineStorage quarantineStorage
	) {
		this.wholesaleFileRepository = wholesaleFileRepository;
		this.userRepository = userRepository;
		this.encodingDetector = encodingDetector;
		this.csvParser = csvParser;
		this.fileParser = fileParser;
		this.previewService = previewService;
		this.uploadAdmissionService = uploadAdmissionService;
		this.quarantineStorage = quarantineStorage;
	}

	@Transactional
	public WholesaleFileResponse upload(
			Long userId,
			MultipartFile file,
			CsvEncoding requestedEncoding,
			String sourceName
	) {
		AcceptedUpload accepted = uploadAdmissionService.admit(file, EnumSet.of(UploadFileType.CSV));
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		Path storedPath = quarantineStorage.store(accepted);
		registerQuarantineCleanup(storedPath);
		try {
			CsvEncoding encoding = encodingDetector.detect(accepted.bytes(), requestedEncoding);
			CsvDocument document = csvParser.parse(encodingDetector.decode(accepted.bytes(), encoding));
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
			WholesaleFile uploaded = WholesaleFile.uploaded(
					user,
					sourceName,
					accepted.originalFilename(),
					storedPath.toString(),
					accepted.bytes().length,
					requestedEncoding,
					encoding,
					rowCount,
					document.header()
			);
			return WholesaleFileResponse.from(wholesaleFileRepository.save(uploaded));
		} catch (RuntimeException exception) {
			quarantineStorage.deleteQuietly(storedPath);
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public WholesaleFileResponse get(Long userId, Long fileId) {
		return WholesaleFileResponse.from(getFile(userId, fileId));
	}

	@Transactional
	public WholesaleUploadPreviewResponse previewUpload(
			Long userId,
			MultipartFile file,
			CsvEncoding requestedEncoding,
			String sourceName
	) {
		AcceptedUpload accepted = uploadAdmissionService.admit(
				file,
				EnumSet.of(UploadFileType.CSV, UploadFileType.XLSX)
		);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		Path storedPath = quarantineStorage.store(accepted);
		registerQuarantineCleanup(storedPath);
		try {
			ParsedWholesaleFile parsedFile = fileParser.parse(
					accepted.originalFilename(),
					accepted.bytes(),
					requestedEncoding
			);
			CsvDocument document = parsedFile.document();
			int rowCount = document.rows().size();
			if (rowCount > user.getPlanCode().csvRowLimit()) {
				throw new BusinessException(
						ErrorCode.CSV_ROW_LIMIT_EXCEEDED,
						"CSV/XLSX row count exceeds current plan limit.",
						"file"
				);
			}
			WholesaleFile uploaded = WholesaleFile.uploaded(
					user,
					sourceName,
					accepted.originalFilename(),
					storedPath.toString(),
					accepted.bytes().length,
					parsedFile.fileType(),
					requestedEncoding,
					parsedFile.detectedEncoding(),
					rowCount,
					document.header()
			);
			WholesaleFile saved = wholesaleFileRepository.save(uploaded);
			WholesaleFilePreviewResponse preview = previewService.toResponse(accepted.originalFilename(), parsedFile);
			return WholesaleUploadPreviewResponse.from(saved, preview);
		} catch (RuntimeException exception) {
			quarantineStorage.deleteQuietly(storedPath);
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public WholesaleFile getFile(Long userId, Long fileId) {
		return wholesaleFileRepository.findByIdAndUserId(fileId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WHOLESALE_FILE_NOT_FOUND));
	}

	private void registerQuarantineCleanup(Path storedPath) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != TransactionSynchronization.STATUS_COMMITTED) {
					quarantineStorage.deleteQuietly(storedPath);
				}
			}
		});
	}
}
