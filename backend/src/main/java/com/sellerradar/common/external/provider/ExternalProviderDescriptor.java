package com.sellerradar.common.external.provider;

import java.util.Set;

public record ExternalProviderDescriptor(
		ExternalProviderMode mode,
		Set<ExternalCapability> capabilities
) {
	public ExternalProviderDescriptor {
		capabilities = Set.copyOf(capabilities);
	}

	public boolean supports(ExternalCapability capability) {
		return capabilities.contains(capability);
	}
}
