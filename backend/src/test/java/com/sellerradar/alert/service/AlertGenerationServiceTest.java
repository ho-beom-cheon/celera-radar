package com.sellerradar.alert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sellerradar.alert.domain.Alert;
import com.sellerradar.alert.domain.AlertRule;
import com.sellerradar.alert.domain.AlertType;
import com.sellerradar.alert.repository.AlertRepository;
import com.sellerradar.alert.repository.AlertRuleRepository;
import com.sellerradar.candidate.domain.CandidateScore;
import com.sellerradar.candidate.domain.CandidateStatus;
import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.domain.RiskLevel;
import com.sellerradar.candidate.repository.ProductCandidateRepository;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.user.domain.User;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertGenerationServiceTest {
	private AlertRuleRepository alertRuleRepository;
	private AlertRepository alertRepository;
	private ProductCandidateRepository candidateRepository;
	private AlertGenerationService service;

	@BeforeEach
	void setUp() {
		alertRuleRepository = mock(AlertRuleRepository.class);
		alertRepository = mock(AlertRepository.class);
		candidateRepository = mock(ProductCandidateRepository.class);
		service = new AlertGenerationService(alertRuleRepository, alertRepository, candidateRepository);
	}

	@Test
	void generateDailyCreatesAlertForMatchingCandidate() {
		AlertRule rule = rule(1L, 10L, 80, "25.0", List.of(CategoryCode.CAR_ACCESSORY), true);
		ProductCandidate candidate = candidate(100L, "car brush", 86, "31.20", CategoryCode.CAR_ACCESSORY, RiskLevel.LOW);
		when(alertRuleRepository.findByActiveTrue()).thenReturn(List.of(rule));
		when(candidateRepository.findByUserIdAndStatusNot(10L, CandidateStatus.EXCLUDED))
				.thenReturn(List.of(candidate));
		when(alertRepository.existsByRuleIdAndCandidateIdAndType(1L, 100L, AlertType.CANDIDATE_SCORE))
				.thenReturn(false);

		AlertGenerationResult result = service.generateDaily();

		assertThat(result.targetRuleCount()).isEqualTo(1);
		assertThat(result.generatedCount()).isEqualTo(1);
		verify(alertRepository).save(any(Alert.class));
	}

	@Test
	void generateDailySkipsDuplicateAlertKey() {
		AlertRule rule = rule(1L, 10L, 80, "20.0", List.of(), true);
		ProductCandidate candidate = candidate(100L, "storage box", 90, "35.00", CategoryCode.HOME_STORAGE, RiskLevel.LOW);
		when(alertRuleRepository.findByActiveTrue()).thenReturn(List.of(rule));
		when(candidateRepository.findByUserIdAndStatusNot(10L, CandidateStatus.EXCLUDED))
				.thenReturn(List.of(candidate));
		when(alertRepository.existsByRuleIdAndCandidateIdAndType(1L, 100L, AlertType.CANDIDATE_SCORE))
				.thenReturn(true);

		AlertGenerationResult result = service.generateDaily();

		assertThat(result.generatedCount()).isZero();
		verify(alertRepository, never()).save(any(Alert.class));
	}

	@Test
	void generateDailySkipsExcludedRiskWhenRuleExcludesRisk() {
		AlertRule rule = rule(1L, 10L, 70, "10.0", List.of(), true);
		ProductCandidate candidate = candidate(100L, "blocked item", 90, "35.00", CategoryCode.CAR_ACCESSORY, RiskLevel.EXCLUDED);
		when(alertRuleRepository.findByActiveTrue()).thenReturn(List.of(rule));
		when(candidateRepository.findByUserIdAndStatusNot(10L, CandidateStatus.EXCLUDED))
				.thenReturn(List.of(candidate));

		AlertGenerationResult result = service.generateDaily();

		assertThat(result.generatedCount()).isZero();
		verify(alertRepository, never()).save(any(Alert.class));
	}

	private AlertRule rule(
			Long ruleId,
			Long userId,
			int minScore,
			String minMarginRate,
			List<CategoryCode> categoryCodes,
			boolean riskExcluded
	) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(userId);
		AlertRule rule = mock(AlertRule.class);
		when(rule.getId()).thenReturn(ruleId);
		when(rule.getUser()).thenReturn(user);
		when(rule.getMinScore()).thenReturn(minScore);
		when(rule.getMinMarginRate()).thenReturn(new BigDecimal(minMarginRate));
		when(rule.categoryCodes()).thenReturn(categoryCodes);
		when(rule.isRiskExcluded()).thenReturn(riskExcluded);
		return rule;
	}

	private ProductCandidate candidate(
			Long candidateId,
			String name,
			int scoreValue,
			String marginRate,
			CategoryCode categoryCode,
			RiskLevel riskLevel
	) {
		CandidateScore score = mock(CandidateScore.class);
		when(score.getOverallScore()).thenReturn(scoreValue);
		when(score.getRiskLevel()).thenReturn(riskLevel);
		ProductCandidate candidate = mock(ProductCandidate.class);
		when(candidate.getId()).thenReturn(candidateId);
		when(candidate.getName()).thenReturn(name);
		when(candidate.getScore()).thenReturn(score);
		when(candidate.getExpectedMarginRate()).thenReturn(new BigDecimal(marginRate));
		when(candidate.getCategoryCode()).thenReturn(categoryCode);
		return candidate;
	}
}
