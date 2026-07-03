package com.sellerradar.shopping.repository;

import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingPriceSnapshotRepository extends JpaRepository<ShoppingPriceSnapshot, Long> {
	Optional<ShoppingPriceSnapshot> findByKeyword_IdAndSearchDateAndSortType(
			Long keywordId,
			LocalDate searchDate,
			String sortType
	);

	@EntityGraph(attributePaths = "topItems")
	Optional<ShoppingPriceSnapshot> findFirstByKeyword_IdOrderBySearchDateDesc(Long keywordId);

	boolean existsByKeyword_IdAndSearchDateAndSortType(Long keywordId, LocalDate searchDate, String sortType);

	default Optional<ShoppingPriceSnapshot> findByKeyword_IdAndBaseDate(Long keywordId, LocalDate baseDate) {
		return findByKeyword_IdAndSearchDateAndSortType(keywordId, baseDate, "sim");
	}

	default Optional<ShoppingPriceSnapshot> findFirstByKeyword_IdOrderByBaseDateDesc(Long keywordId) {
		return findFirstByKeyword_IdOrderBySearchDateDesc(keywordId);
	}

	default boolean existsByKeyword_IdAndBaseDate(Long keywordId, LocalDate baseDate) {
		return existsByKeyword_IdAndSearchDateAndSortType(keywordId, baseDate, "sim");
	}
}
