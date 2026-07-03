package com.sellerradar.shopping.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordPriority;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ShoppingPriceSnapshotRepositoryTest {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private ShoppingPriceSnapshotRepository snapshotRepository;

	@Test
	void preventsDuplicateSnapshotForSameKeywordAndBaseDate() {
		User user = userRepository.save(User.create("snapshot-owner@example.com", "{bcrypt}hash"));
		Keyword keyword = keywordRepository.save(Keyword.create(
				user,
				"차량용 수납함",
				"차량용 수납함",
				CategoryCode.CAR_ACCESSORY,
				KeywordPriority.MEDIUM
		));
		LocalDate baseDate = LocalDate.of(2026, 7, 2);

		snapshotRepository.saveAndFlush(ShoppingPriceSnapshot.create(
				keyword,
				baseDate,
				100L,
				1000,
				2000,
				1500,
				"{}"
		));

		assertThatThrownBy(() -> snapshotRepository.saveAndFlush(ShoppingPriceSnapshot.create(
				keyword,
				baseDate,
				200L,
				2000,
				3000,
				2500,
				"{}"
		))).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void allowsSameKeywordAndSearchDateWhenSortTypeDiffers() {
		User user = userRepository.save(User.create("snapshot-sort-owner@example.com", "{bcrypt}hash"));
		Keyword keyword = keywordRepository.save(Keyword.create(
				user,
				"차량용 수납함",
				"차량용 수납함",
				CategoryCode.CAR_ACCESSORY,
				KeywordPriority.MEDIUM
		));
		LocalDate searchDate = LocalDate.of(2026, 7, 2);

		snapshotRepository.saveAndFlush(ShoppingPriceSnapshot.createSuccess(
				keyword,
				searchDate,
				keyword.getKeyword(),
				"sim",
				100,
				100,
				1000,
				2000,
				1500,
				1500,
				"{}",
				null,
				false
		));
		snapshotRepository.saveAndFlush(ShoppingPriceSnapshot.createSuccess(
				keyword,
				searchDate,
				keyword.getKeyword(),
				"date",
				100,
				100,
				1000,
				2000,
				1500,
				1500,
				"{}",
				null,
				false
		));

		assertThat(snapshotRepository.findByKeyword_IdAndSearchDateAndSortType(keyword.getId(), searchDate, "sim"))
				.isPresent();
		assertThat(snapshotRepository.findByKeyword_IdAndSearchDateAndSortType(keyword.getId(), searchDate, "date"))
				.isPresent();
	}
}
