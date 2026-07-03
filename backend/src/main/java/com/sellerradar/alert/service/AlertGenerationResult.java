package com.sellerradar.alert.service;

public record AlertGenerationResult(
		int targetRuleCount,
		int generatedCount
) {
}
