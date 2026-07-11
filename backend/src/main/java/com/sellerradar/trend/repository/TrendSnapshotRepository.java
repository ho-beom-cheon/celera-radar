package com.sellerradar.trend.repository;

import com.sellerradar.trend.domain.TrendSnapshot;
import com.sellerradar.trend.domain.TrendTimeUnit;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrendSnapshotRepository extends JpaRepository<TrendSnapshot, Long> {
	Optional<TrendSnapshot> findByKeyword_IdAndSnapshotDateAndDataPeriodAndTimeUnit(
			Long keywordId,
			LocalDate snapshotDate,
			LocalDate dataPeriod,
			TrendTimeUnit timeUnit
	);

	Optional<TrendSnapshot> findFirstByKeyword_IdAndTimeUnitOrderBySnapshotDateDesc(
			Long keywordId,
			TrendTimeUnit timeUnit
	);

	List<TrendSnapshot> findByKeyword_IdAndSnapshotDateAndTimeUnitOrderByDataPeriodAsc(
			Long keywordId,
			LocalDate snapshotDate,
			TrendTimeUnit timeUnit
	);
}
