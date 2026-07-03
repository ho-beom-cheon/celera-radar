package com.sellerradar.category.config;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.category.domain.CategoryMaster;
import com.sellerradar.category.domain.RiskCategoryRule;
import com.sellerradar.category.domain.RiskHandlingType;
import com.sellerradar.category.repository.CategoryMasterRepository;
import com.sellerradar.category.repository.RiskCategoryRuleRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CategorySeedData implements ApplicationRunner {
	private final CategoryMasterRepository categoryMasterRepository;
	private final RiskCategoryRuleRepository riskCategoryRuleRepository;

	public CategorySeedData(
			CategoryMasterRepository categoryMasterRepository,
			RiskCategoryRuleRepository riskCategoryRuleRepository
	) {
		this.categoryMasterRepository = categoryMasterRepository;
		this.riskCategoryRuleRepository = riskCategoryRuleRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		seedCategories();
		seedRiskRules();
	}

	private void seedCategories() {
		CategoryCode[] categoryCodes = CategoryCode.values();
		for (int i = 0; i < categoryCodes.length; i++) {
			CategoryCode code = categoryCodes[i];
			if (!categoryMasterRepository.existsById(code)) {
				categoryMasterRepository.save(CategoryMaster.active(code, (i + 1) * 10));
			}
		}
	}

	private void seedRiskRules() {
		List<RiskCategoryRule> rules = List.of(
				RiskCategoryRule.active("건강기능식품", RiskHandlingType.EXCLUDE, "광고/인허가 리스크", 10),
				RiskCategoryRule.active("어린이제품", RiskHandlingType.EXCLUDE, "어린이제품 안전관리 리스크", 20),
				RiskCategoryRule.active("의료기기", RiskHandlingType.EXCLUDE, "허가/광고 리스크", 30),
				RiskCategoryRule.active("화장품", RiskHandlingType.EXCLUDE, "책임판매/표시 리스크", 40),
				RiskCategoryRule.active("식품", RiskHandlingType.EXCLUDE, "신고/표시/보관 리스크", 50),
				RiskCategoryRule.active("배터리", RiskHandlingType.EXCLUDE, "KC/안전 인증 리스크", 60),
				RiskCategoryRule.active("충전기", RiskHandlingType.EXCLUDE, "KC/안전 인증 리스크", 70),
				RiskCategoryRule.active("전기", RiskHandlingType.EXCLUDE, "KC/안전 인증 리스크", 80),
				RiskCategoryRule.active("대형가구", RiskHandlingType.CAUTION, "배송/파손/반품 리스크", 90),
				RiskCategoryRule.active("의류", RiskHandlingType.CAUTION, "사이즈/반품 리스크", 100),
				RiskCategoryRule.active("신발", RiskHandlingType.CAUTION, "사이즈/반품 리스크", 110)
		);
		rules.stream()
				.filter(rule -> !riskCategoryRuleRepository.existsByRiskKeyword(rule.getRiskKeyword()))
				.forEach(riskCategoryRuleRepository::save);
	}
}
