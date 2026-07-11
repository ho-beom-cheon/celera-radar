package com.sellerradar.wholesale.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.wholesale.upload.XlsxSecurityProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;

@Component
public class XlsxZipGuard {
	private static final int BUFFER_SIZE = 8192;

	private final XlsxSecurityProperties properties;

	public XlsxZipGuard(XlsxSecurityProperties properties) {
		this.properties = properties;
	}

	public void validate(byte[] bytes) {
		int entryCount = 0;
		long totalUncompressed = 0;
		byte[] buffer = new byte[BUFFER_SIZE];
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				entryCount++;
				if (entryCount > properties.maxZipEntries()) {
					throw invalid("XLSX contains too many ZIP entries.");
				}
				validateEntryName(entry.getName());
				long entryUncompressed = 0;
				int read;
				while ((read = zip.read(buffer)) != -1) {
					entryUncompressed += read;
					totalUncompressed += read;
					if (totalUncompressed > properties.maxUncompressedSize().toBytes()) {
						throw invalid("XLSX uncompressed size exceeds the security limit.");
					}
				}
				validateInflateRatio(entry, entryUncompressed);
				zip.closeEntry();
			}
		} catch (BusinessException exception) {
			throw exception;
		} catch (IOException exception) {
			throw invalid("XLSX ZIP structure is invalid.");
		}
		if (entryCount == 0) {
			throw invalid("XLSX ZIP archive is empty.");
		}
	}

	private void validateEntryName(String name) {
		String normalized = name.replace('\\', '/');
		if (normalized.startsWith("/") || normalized.contains("../") || normalized.equals("..")) {
			throw invalid("XLSX ZIP entry path is invalid.");
		}
	}

	private void validateInflateRatio(ZipEntry entry, long uncompressedSize) {
		if (uncompressedSize == 0 || entry.isDirectory()) {
			return;
		}
		long compressedSize = entry.getCompressedSize();
		if (compressedSize <= 0) {
			throw invalid("XLSX ZIP entry compressed size is invalid.");
		}
		double ratio = (double) compressedSize / uncompressedSize;
		if (ratio < properties.minInflateRatio()) {
			throw invalid("XLSX ZIP inflate ratio is below the security limit.");
		}
	}

	private BusinessException invalid(String message) {
		return new BusinessException(ErrorCode.CSV_INVALID_FORMAT, message, "file");
	}
}
