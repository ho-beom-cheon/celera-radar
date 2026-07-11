package com.sellerradar.trend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.category.domain.CategoryCode;
import org.junit.jupiter.api.Test;

class NaverShoppingCategoryCodeResolverTest {
	private final NaverShoppingCategoryCodeResolver resolver = new NaverShoppingCategoryCodeResolver();

	@Test
	void mapsInternalCategoriesToNaverTopLevelCategories() {
		assertThat(resolver.resolve(CategoryCode.CAR_ACCESSORY)).isEqualTo("50000008");
		assertThat(resolver.resolve(CategoryCode.HOME_STORAGE)).isEqualTo("50000004");
		assertThat(resolver.resolve(CategoryCode.TRAVEL_ORGANIZER)).isEqualTo("50000001");
		assertThat(resolver.resolve(CategoryCode.CAMPING_PICNIC)).isEqualTo("50000007");
	}

	@Test
	void preservesOfficialCodeAndFallsBackToLivingHealth() {
		assertThat(resolver.resolve("50000000")).isEqualTo("50000000");
		assertThat(resolver.resolve((String) null)).isEqualTo("50000008");
		assertThat(resolver.resolve("UNKNOWN")).isEqualTo("50000008");
	}
}
