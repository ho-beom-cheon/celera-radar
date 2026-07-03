package com.sellerradar.shopping.repository;

import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingPriceSnapshotRepository extends JpaRepository<ShoppingPriceSnapshot, Long> {
	Optional<ShoppingPriceSnapshot> findByKeyword_IdAndBaseDate(Long keywordId, LocalDate baseDate);

	@EntityGraph(attributePaths = "topItems")
	Optional<ShoppingPriceSnapshot> findFirstByKeyword_IdOrderByBaseDateDesc(Long keywordId);

	boolean existsByKeyword_IdAndBaseDate(Long keywordId, LocalDate baseDate);
}
