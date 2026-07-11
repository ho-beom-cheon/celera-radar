package com.sellerradar.wholesale.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "seller-radar.wholesale.upload-security")
public record UploadSecurityProperties(
		String quarantineDirectory,
		DataSize maxFileSize
) {
}
