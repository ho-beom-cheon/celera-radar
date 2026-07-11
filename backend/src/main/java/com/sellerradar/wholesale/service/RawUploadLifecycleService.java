package com.sellerradar.wholesale.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.wholesale.domain.WholesaleFile;
import com.sellerradar.wholesale.dto.WholesaleFileResponse;
import com.sellerradar.wholesale.repository.WholesaleFileRepository;
import com.sellerradar.wholesale.upload.RawUploadLifecycleProperties;
import com.sellerradar.wholesale.upload.UploadQuarantineStorage;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RawUploadLifecycleService {
	private final WholesaleFileRepository repository;
	private final UploadQuarantineStorage quarantineStorage;
	private final RawUploadLifecycleProperties properties;

	public RawUploadLifecycleService(
			WholesaleFileRepository repository,
			UploadQuarantineStorage quarantineStorage,
			RawUploadLifecycleProperties properties
	) {
		this.repository = repository;
		this.quarantineStorage = quarantineStorage;
		this.properties = properties;
	}

	@Transactional
	public WholesaleFileResponse deleteForUser(Long userId, Long fileId) {
		WholesaleFile upload = repository.findByIdAndUserId(fileId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WHOLESALE_FILE_NOT_FOUND));
		deleteRawObject(upload, OffsetDateTime.now(ZoneOffset.UTC));
		return WholesaleFileResponse.from(upload);
	}

	@Scheduled(
			cron = "${seller-radar.wholesale.raw-lifecycle.cleanup-cron:0 0 3 * * *}",
			zone = "UTC"
	)
	@Transactional
	public void cleanupExpired() {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		List<WholesaleFile> expired = repository
				.findByRawDeletedAtIsNullAndRawExpiresAtLessThanEqualOrderByRawExpiresAtAsc(
						now,
						PageRequest.of(0, properties.cleanupBatchSize())
				);
		expired.forEach(upload -> deleteRawObject(upload, now));
	}

	private void deleteRawObject(WholesaleFile upload, OffsetDateTime attemptedAt) {
		if (upload.getRawDeletedAt() != null) {
			return;
		}
		try {
			quarantineStorage.delete(Path.of(upload.getStoredPath()));
			upload.markRawDeleted(attemptedAt);
		} catch (IOException | InvalidPathException exception) {
			upload.markRawDeleteFailed(attemptedAt);
		}
	}
}
