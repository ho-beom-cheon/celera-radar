package com.sellerradar.wholesale.service;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ProductNameNormalizer {
	public String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value.trim()
				.toLowerCase(Locale.ROOT)
				.replaceAll("<[^>]+>", "")
				.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHangul}]+", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}
}
