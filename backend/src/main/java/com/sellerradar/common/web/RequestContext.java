package com.sellerradar.common.web;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestContext {
	public static final String REQUEST_ID_HEADER = "X-Request-Id";
	public static final String REQUEST_ID_ATTRIBUTE = "requestId";

	private RequestContext() {
	}

	public static String requestId(HttpServletRequest request) {
		Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
		if (requestId instanceof String value && !value.isBlank()) {
			return value;
		}
		return null;
	}
}
