package com.sellerradar.alert.service;

import com.sellerradar.alert.domain.Alert;
import com.sellerradar.alert.domain.AlertRule;
import com.sellerradar.alert.domain.AlertType;
import com.sellerradar.alert.repository.AlertRepository;
import com.sellerradar.alert.repository.AlertRuleRepository;
import com.sellerradar.candidate.domain.CandidateStatus;
import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.domain.RiskLevel;
import com.sellerradar.candidate.repository.ProductCandidateRepository;
import com.sellerradar.category.domain.CategoryCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertGenerationService {
	private final AlertRuleRepository alertRuleRepository;
	private final AlertRepository alertRepository;
	private final ProductCandidateRepository candidateRepository;

	public AlertGenerationService(
			AlertRuleRepository alertRuleRepository,
			AlertRepository alertRepository,
			ProductCandidateRepository candidateRepository
	) {
		this.alertRuleRepository = alertRuleRepository;
		this.alertRepository = alertRepository;
		this.candidateRepository = candidateRepository;
	}

	@Transactional
	public AlertGenerationResult generateDaily() {
		List<AlertRule> rules = alertRuleRepository.findByActiveTrue();
		int generatedCount = 0;
		for (AlertRule rule : rules) {
			generatedCount += generateForRule(rule);
		}
		return new AlertGenerationResult(rules.size(), generatedCount);
	}

	private int generateForRule(AlertRule rule) {
		List<ProductCandidate> candidates = candidateRepository.findByUserIdAndStatusNot(
				rule.getUser().getId(),
				CandidateStatus.EXCLUDED
		);
		int generatedCount = 0;
		for (ProductCandidate candidate : candidates) {
			if (!matches(rule, candidate)) {
				continue;
			}
			if (alertRepository.existsByRuleIdAndCandidateIdAndType(
					rule.getId(),
					candidate.getId(),
					AlertType.CANDIDATE_SCORE
			)) {
				continue;
			}
			alertRepository.save(Alert.candidateScore(rule, candidate));
			generatedCount++;
		}
		return generatedCount;
	}

	private boolean matches(AlertRule rule, ProductCandidate candidate) {
		if (candidate.getScore() == null) {
			return false;
		}
		if (candidate.getScore().getOverallScore() < rule.getMinScore()) {
			return false;
		}
		if (candidate.getExpectedMarginRate().compareTo(rule.getMinMarginRate()) < 0) {
			return false;
		}
		List<CategoryCode> categoryCodes = rule.categoryCodes();
		if (!categoryCodes.isEmpty() && !categoryCodes.contains(candidate.getCategoryCode())) {
			return false;
		}
		return !rule.isRiskExcluded() || candidate.getScore().getRiskLevel() != RiskLevel.EXCLUDED;
	}
}
