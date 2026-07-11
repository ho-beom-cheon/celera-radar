package com.sellerradar.wholesale.upload;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seller-radar.wholesale.raw-lifecycle")
public record RawUploadLifecycleProperties(
		Duration retention,
		int cleanupBatchSize
) {
}
