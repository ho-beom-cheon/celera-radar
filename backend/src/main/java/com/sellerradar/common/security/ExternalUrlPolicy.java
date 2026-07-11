package com.sellerradar.common.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ExternalUrlPolicy {
	public Optional<String> normalize(String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			return Optional.empty();
		}
		String value = rawUrl.strip();
		if (value.startsWith("//") || value.codePoints().anyMatch(Character::isISOControl)) {
			return Optional.empty();
		}
		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if (!(scheme.equals("http") || scheme.equals("https"))
					|| uri.getHost() == null
					|| uri.getUserInfo() != null) {
				return Optional.empty();
			}
			return Optional.of(uri.toASCIIString());
		} catch (URISyntaxException exception) {
			return Optional.empty();
		}
	}
}
