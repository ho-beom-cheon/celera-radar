package com.sellerradar.wholesale.service;

import java.math.BigDecimal;

public final class WholesaleNumberParser {
	private WholesaleNumberParser() {
	}

	public static BigDecimal parseBigDecimal(String value) {
		String normalized = normalize(value);
		if (normalized.isBlank()) {
			return null;
		}
		try {
			return new BigDecimal(normalized);
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	public static Long parseLong(String value) {
		BigDecimal parsed = parseBigDecimal(value);
		if (parsed == null) {
			return null;
		}
		try {
			return parsed.longValueExact();
		} catch (ArithmeticException exception) {
			return null;
		}
	}

	public static Integer parsePositiveInteger(String value) {
		Integer parsed = parseNonNegativeInteger(value);
		if (parsed == null || parsed <= 0) {
			return null;
		}
		return parsed;
	}

	public static Integer parseNonNegativeInteger(String value) {
		Long parsed = parseLong(value);
		if (parsed == null || parsed < 0 || parsed > Integer.MAX_VALUE) {
			return null;
		}
		return parsed.intValue();
	}

	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("\\", "")
				.replaceAll("[,\\s\\u20A9\\uFFE6\\uC6D0]", "");
	}
}
