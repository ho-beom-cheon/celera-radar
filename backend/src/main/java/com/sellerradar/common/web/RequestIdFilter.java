package com.sellerradar.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
	private static final int REQUEST_ID_LENGTH = 24;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String requestId = resolveRequestId(request);
		request.setAttribute(RequestContext.REQUEST_ID_ATTRIBUTE, requestId);
		response.setHeader(RequestContext.REQUEST_ID_HEADER, requestId);
		MDC.put(RequestContext.REQUEST_ID_ATTRIBUTE, requestId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(RequestContext.REQUEST_ID_ATTRIBUTE);
		}
	}

	private String resolveRequestId(HttpServletRequest request) {
		String headerValue = request.getHeader(RequestContext.REQUEST_ID_HEADER);
		if (headerValue != null && !headerValue.isBlank()) {
			return headerValue;
		}
		return "req_" + UUID.randomUUID()
				.toString()
				.replace("-", "")
				.substring(0, REQUEST_ID_LENGTH);
	}
}
