package com.sellerradar.shopping.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.shopping.domain.ShoppingCompetitionLevel;
import org.junit.jupiter.api.Test;

class CompetitionAnalyzerTest {
	private final CompetitionAnalyzer analyzer = new CompetitionAnalyzer();

	@Test
	void returnsUnknownWhenTotalCountIsMissingOrItemsAreEmpty() {
		assertThat(analyzer.analyze(null, 10)).isEqualTo(ShoppingCompetitionLevel.UNKNOWN);
		assertThat(analyzer.analyze(10_000, 0)).isEqualTo(ShoppingCompetitionLevel.UNKNOWN);
	}

	@Test
	void classifiesCompetitionByTotalCount() {
		assertThat(analyzer.analyze(2_999, 10)).isEqualTo(ShoppingCompetitionLevel.LOW);
		assertThat(analyzer.analyze(3_000, 10)).isEqualTo(ShoppingCompetitionLevel.MEDIUM);
		assertThat(analyzer.analyze(9_999, 10)).isEqualTo(ShoppingCompetitionLevel.MEDIUM);
		assertThat(analyzer.analyze(10_000, 10)).isEqualTo(ShoppingCompetitionLevel.HIGH);
		assertThat(analyzer.analyze(50_000, 10)).isEqualTo(ShoppingCompetitionLevel.HIGH);
	}
}
