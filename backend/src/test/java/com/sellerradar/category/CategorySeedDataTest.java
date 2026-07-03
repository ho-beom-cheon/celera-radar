package com.sellerradar.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.category.domain.CategoryMaster;
import com.sellerradar.category.domain.RiskCategoryRule;
import com.sellerradar.category.domain.RiskHandlingType;
import com.sellerradar.category.repository.CategoryMasterRepository;
import com.sellerradar.category.repository.RiskCategoryRuleRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CategorySeedDataTest {
	@Autowired
	private CategoryMasterRepository categoryMasterRepository;

	@Autowired
	private RiskCategoryRuleRepository riskCategoryRuleRepository;

	@Test
	void seedsAllInitialCategoryCodes() {
		List<CategoryMaster> categories = categoryMasterRepository.findByActiveTrueOrderBySortOrderAsc();

		assertThat(categories).hasSize(CategoryCode.values().length);
		assertThat(categories)
				.extracting(CategoryMaster::getCode)
				.containsExactly(CategoryCode.values());
		assertThat(categories)
				.allSatisfy(category -> {
					assertThat(category.getDisplayName()).isNotBlank();
					assertThat(category.isActive()).isTrue();
					assertThat(category.getCreatedAt()).isNotNull();
					assertThat(category.getUpdatedAt()).isNotNull();
				});
	}

	@Test
	void seedsRiskCategoryRules() {
		List<RiskCategoryRule> rules = riskCategoryRuleRepository.findByActiveTrueOrderBySortOrderAsc();
		Set<String> keywords = rules.stream()
				.map(RiskCategoryRule::getRiskKeyword)
				.collect(Collectors.toSet());

		assertThat(rules).hasSize(11);
		assertThat(keywords).contains(
				"식품",
				"건강기능식품",
				"화장품",
				"의료기기",
				"전기",
				"배터리",
				"충전기",
				"어린이제품",
				"의류",
				"신발",
				"대형가구"
		);
		assertThat(rules)
				.filteredOn(rule -> Set.of("식품", "건강기능식품", "화장품", "의료기기", "전기", "배터리", "충전기", "어린이제품")
						.contains(rule.getRiskKeyword()))
				.allSatisfy(rule -> assertThat(rule.getHandlingType()).isEqualTo(RiskHandlingType.EXCLUDE));
		assertThat(rules)
				.filteredOn(rule -> Set.of("의류", "신발", "대형가구").contains(rule.getRiskKeyword()))
				.allSatisfy(rule -> assertThat(rule.getHandlingType()).isEqualTo(RiskHandlingType.CAUTION));
	}
}
