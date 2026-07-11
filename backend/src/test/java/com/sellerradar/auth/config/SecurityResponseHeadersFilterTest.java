package com.sellerradar.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityResponseHeadersFilterTest {
	@Test
	void usesReportOnlyCspAndRestrictiveBrowserHeadersByDefault() throws Exception {
		MockHttpServletResponse response = filter(false, "").apply();

		assertThat(response.getHeader("Content-Security-Policy-Report-Only")).contains("default-src 'none'");
		assertThat(response.getHeader("Content-Security-Policy")).isNull();
		assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
		assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
		assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
		assertThat(response.getHeader("Permissions-Policy")).contains("camera=()", "payment=()");
		assertThat(response.getHeader("Cross-Origin-Opener-Policy")).isEqualTo("same-origin");
	}

	@Test
	void usesEnforcedCspAndOptionalReportUriInProductionMode() throws Exception {
		MockHttpServletResponse response = filter(true, "https://reports.example.com/csp").apply();

		assertThat(response.getHeader("Content-Security-Policy"))
				.contains("default-src 'none'", "report-uri https://reports.example.com/csp");
		assertThat(response.getHeader("Content-Security-Policy-Report-Only")).isNull();
		assertThat(response.getHeader("Strict-Transport-Security"))
				.isEqualTo("max-age=31536000; includeSubDomains");
	}

	@Test
	void rejectsWildcardOriginWithCredentialedCors() {
		assertThatThrownBy(() -> new WebSecurityProperties(
				false,
				"default-src 'none'",
				"",
				List.of("*")
		)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsCorsOriginWithPathOrUserInfo() {
		assertThatThrownBy(() -> new WebSecurityProperties(
				false,
				"default-src 'none'",
				"",
				List.of("https://user@example.com/path")
		)).isInstanceOf(IllegalArgumentException.class);
	}

	private FilterCall filter(boolean enforce, String reportUri) {
		WebSecurityProperties properties = new WebSecurityProperties(
				enforce,
				"default-src 'none'; frame-ancestors 'none'",
				reportUri,
				List.of()
		);
		SecurityResponseHeadersFilter filter = new SecurityResponseHeadersFilter(properties);
		return () -> {
			MockHttpServletResponse response = new MockHttpServletResponse();
			filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
			return response;
		};
	}

	@FunctionalInterface
	private interface FilterCall {
		MockHttpServletResponse apply() throws Exception;
	}
}
