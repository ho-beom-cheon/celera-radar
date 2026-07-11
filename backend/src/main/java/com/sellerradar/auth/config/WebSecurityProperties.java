package com.sellerradar.auth.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seller-radar.web-security")
public record WebSecurityProperties(
		boolean cspEnforce,
		String cspPolicy,
		String cspReportUri,
		List<String> allowedOrigins
) {
	public WebSecurityProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
		if (cspPolicy == null || cspPolicy.isBlank()) {
			throw new IllegalArgumentException("CSP policy must not be blank.");
		}
		if (allowedOrigins.stream().map(String::strip).anyMatch("*"::equals)) {
			throw new IllegalArgumentException("Credentialed CORS must not allow wildcard origins.");
		}
		allowedOrigins.stream()
				.map(String::strip)
				.filter(origin -> !origin.isBlank())
				.forEach(WebSecurityProperties::validateOrigin);
		if (cspReportUri != null && !cspReportUri.isBlank()) {
			validateUrl(cspReportUri.strip(), "CSP report URI");
		}
	}

	private static void validateOrigin(String origin) {
		URI uri = validateUrl(origin, "CORS origin");
		if (uri.getUserInfo() != null
				|| (uri.getPath() != null && !(uri.getPath().isBlank() || uri.getPath().equals("/")))
				|| uri.getQuery() != null
				|| uri.getFragment() != null) {
			throw new IllegalArgumentException("CORS origin must contain only scheme, host, and optional port.");
		}
	}

	private static URI validateUrl(String value, String label) {
		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if (!(scheme.equals("http") || scheme.equals("https"))
					|| uri.getHost() == null
					|| uri.getUserInfo() != null
					|| value.codePoints().anyMatch(Character::isISOControl)) {
				throw new IllegalArgumentException(label + " must be an absolute HTTP(S) URL without user-info.");
			}
			return uri;
		} catch (URISyntaxException exception) {
			throw new IllegalArgumentException(label + " is invalid.", exception);
		}
	}
}
