package com.sellerradar.keyword.service;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class KeywordNormalizer {
	public String displayKeyword(String keyword) {
		return keyword.trim().replaceAll("\\s+", " ");
	}

	public String normalize(String keyword) {
		return displayKeyword(keyword).toLowerCase(Locale.ROOT);
	}
}
