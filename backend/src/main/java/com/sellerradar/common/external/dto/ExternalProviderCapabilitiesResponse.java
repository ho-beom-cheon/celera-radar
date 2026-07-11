package com.sellerradar.common.external.dto;

import com.sellerradar.common.external.provider.ExternalProviderDescriptor;
import java.util.Set;

public record ExternalProviderCapabilitiesResponse(
		String provider,
		String mode,
		Set<String> capabilities
) {
	public static ExternalProviderCapabilitiesResponse naver(ExternalProviderDescriptor descriptor) {
		return new ExternalProviderCapabilitiesResponse(
				"NAVER",
				descriptor.mode().name(),
				descriptor.capabilities().stream()
						.map(Enum::name)
						.collect(java.util.stream.Collectors.toUnmodifiableSet())
		);
	}
}
