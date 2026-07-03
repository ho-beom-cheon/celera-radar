package com.sellerradar.trend.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordPriority;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.trend.domain.TrendSnapshot;
import com.sellerradar.trend.domain.TrendTimeUnit;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TrendSnapshotRepositoryTest {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private TrendSnapshotRepository trendSnapshotRepository;

	@Test
	void preventsDuplicateSnapshotForSameKeywordPeriodAndTimeUnit() {
		User user = userRepository.save(User.create("trend-owner@example.com", "{bcrypt}hash"));
		Keyword keyword = keywordRepository.save(Keyword.create(
				user,
				"차량용 수납함",
				"차량용 수납함",
				CategoryCode.CAR_ACCESSORY,
				KeywordPriority.MEDIUM
		));
		LocalDate period = LocalDate.of(2026, 7, 1);

		trendSnapshotRepository.saveAndFlush(TrendSnapshot.create(
				keyword,
				period,
				TrendTimeUnit.DATE,
				new BigDecimal("82.3000")
		));

		assertThatThrownBy(() -> trendSnapshotRepository.saveAndFlush(TrendSnapshot.create(
				keyword,
				period,
				TrendTimeUnit.DATE,
				new BigDecimal("91.4000")
		))).isInstanceOf(DataIntegrityViolationException.class);
	}
}
