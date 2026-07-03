package com.sellerradar.keyword.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class KeywordRepositoryTest {
	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void saveKeywordUsesDatabaseDesignFields() {
		User user = userRepository.save(User.create("keyword-domain@example.com", "{bcrypt}hash"));
		Keyword keyword = Keyword.create(
				user,
				"car storage box",
				"car storage box",
				"Car Gear"
		);

		Keyword savedKeyword = keywordRepository.saveAndFlush(keyword);

		assertThat(savedKeyword.getId()).isNotNull();
		assertThat(savedKeyword.getCategory()).isEqualTo("Car Gear");
		assertThat(savedKeyword.isActive()).isTrue();
		assertThat(savedKeyword.getStatus()).isEqualTo(KeywordStatus.ACTIVE);
		assertThat(savedKeyword.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING);
		assertThat(savedKeyword.getCreatedAt()).isNotNull();
		assertThat(savedKeyword.getUpdatedAt()).isNotNull();
		assertThat(savedKeyword.getDeletedAt()).isNull();
	}

	@Test
	void softDeletedKeywordIsExcludedFromActiveQueries() {
		User user = userRepository.save(User.create("keyword-delete@example.com", "{bcrypt}hash"));
		Keyword keyword = keywordRepository.saveAndFlush(Keyword.create(
				user,
				"car storage box",
				"car storage box",
				"Car Gear"
		));

		keyword.delete();
		keywordRepository.saveAndFlush(keyword);

		assertThat(keyword.getStatus()).isEqualTo(KeywordStatus.DELETED);
		assertThat(keyword.isActive()).isFalse();
		assertThat(keyword.getDeletedAt()).isNotNull();
		assertThat(keywordRepository.existsByUserIdAndNormalizedKeywordAndActiveTrueAndDeletedAtIsNull(
				user.getId(),
				"car storage box"
		)).isFalse();
		assertThat(keywordRepository.countByUserIdAndActiveTrueAndDeletedAtIsNull(user.getId())).isZero();
	}

	@Test
	void analysisSuccessUpdatesStatusAndSnapshotDate() {
		User user = userRepository.save(User.create("keyword-analysis@example.com", "{bcrypt}hash"));
		Keyword keyword = Keyword.create(
				user,
				"car storage box",
				"car storage box",
				"Car Gear"
		);

		OffsetDateTime analyzedAt = OffsetDateTime.parse("2026-07-03T10:15:00+09:00");
		keyword.markAnalyzed(analyzedAt);
		keyword.updateLastSnapshotDate(LocalDate.of(2026, 7, 3));

		assertThat(keyword.getAnalysisStatus()).isEqualTo(AnalysisStatus.SUCCESS);
		assertThat(keyword.getLastAnalyzedAt()).isEqualTo(analyzedAt);
		assertThat(keyword.getLastSnapshotDate()).isEqualTo(LocalDate.of(2026, 7, 3));
	}
}
