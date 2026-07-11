package com.sellerradar.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecurityResponseHeadersFilter extends OncePerRequestFilter {
	private static final String CSP_HEADER = "Content-Security-Policy";
	private static final String CSP_REPORT_ONLY_HEADER = "Content-Security-Policy-Report-Only";

	private final WebSecurityProperties properties;

	public SecurityResponseHeadersFilter(WebSecurityProperties properties) {
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Referrer-Policy", "no-referrer");
		response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
		response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
		if (properties.cspEnforce()) {
			response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
		}
		String headerName = properties.cspEnforce() ? CSP_HEADER : CSP_REPORT_ONLY_HEADER;
		response.setHeader(headerName, contentSecurityPolicy());
		filterChain.doFilter(request, response);
	}

	private String contentSecurityPolicy() {
		String policy = properties.cspPolicy().strip();
		String reportUri = properties.cspReportUri();
		if (reportUri == null || reportUri.isBlank()) {
			return policy;
		}
		return policy + "; report-uri " + reportUri.strip();
	}
}
