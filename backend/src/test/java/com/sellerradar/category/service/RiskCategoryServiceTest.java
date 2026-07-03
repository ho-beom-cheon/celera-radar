package com.sellerradar.category.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.category.domain.RiskHandlingType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RiskCategoryServiceTest {
	@Autowired
	private RiskCategoryService riskCategoryService;

	@Test
	void evaluatesExcludedCategoryBySpecificRiskKeyword() {
		RiskCategoryDecision decision = riskCategoryService.evaluate("생활/건강 > 건강기능식품 > 비타민");

		assertThat(decision.risk()).isTrue();
		assertThat(decision.excluded()).isTrue();
		assertThat(decision.handlingType()).isEqualTo(RiskHandlingType.EXCLUDE);
		assertThat(decision.matchedKeyword()).isEqualTo("건강기능식품");
		assertThat(decision.reason()).isEqualTo("광고/인허가 리스크");
	}

	@Test
	void evaluatesCautionCategory() {
		RiskCategoryDecision decision = riskCategoryService.evaluate("패션의류 > 여성의류 > 셔츠");

		assertThat(decision.risk()).isTrue();
		assertThat(decision.excluded()).isFalse();
		assertThat(decision.handlingType()).isEqualTo(RiskHandlingType.CAUTION);
		assertThat(decision.matchedKeyword()).isEqualTo("의류");
		assertThat(decision.reason()).isEqualTo("사이즈/반품 리스크");
	}

	@Test
	void evaluatesSafeCategory() {
		RiskCategoryDecision decision = riskCategoryService.evaluate("생활/건강 > 수납/정리 > 리빙박스");

		assertThat(decision.risk()).isFalse();
		assertThat(decision.excluded()).isFalse();
		assertThat(decision.handlingType()).isNull();
		assertThat(decision.matchedKeyword()).isNull();
		assertThat(decision.reason()).isNull();
	}

	@Test
	void blankCategoryIsSafe() {
		RiskCategoryDecision decision = riskCategoryService.evaluate(" ");

		assertThat(decision.risk()).isFalse();
		assertThat(decision.excluded()).isFalse();
	}
}
