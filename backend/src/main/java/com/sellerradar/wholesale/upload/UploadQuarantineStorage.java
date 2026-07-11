package com.sellerradar.wholesale.upload;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UploadQuarantineStorage {
	private final Path quarantineRoot;

	public UploadQuarantineStorage(UploadSecurityProperties properties) {
		this.quarantineRoot = Path.of(properties.quarantineDirectory()).toAbsolutePath().normalize();
	}

	public Path store(AcceptedUpload upload) {
		try {
			Files.createDirectories(quarantineRoot);
			Path storedPath = quarantineRoot.resolve(UUID.randomUUID() + upload.extension()).normalize();
			if (!storedPath.startsWith(quarantineRoot)) {
				throw new IOException("Quarantine path escaped its root.");
			}
			Files.write(storedPath, upload.bytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			return storedPath;
		} catch (IOException exception) {
			throw new BusinessException(ErrorCode.CSV_INVALID_FORMAT, "Upload file could not be quarantined.", "file");
		}
	}

	public void deleteQuietly(Path storedPath) {
		if (storedPath == null) {
			return;
		}
		Path normalized = storedPath.toAbsolutePath().normalize();
		if (!normalized.startsWith(quarantineRoot)) {
			return;
		}
		try {
			Files.deleteIfExists(normalized);
		} catch (IOException ignored) {
			// R0-06 adds lifecycle reconciliation and deletion observability.
		}
	}
}
