package com.sellerradar.smartstore.commission.repository;

import com.sellerradar.smartstore.commission.domain.NaverCommissionSnapshot;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NaverCommissionSnapshotRepository extends JpaRepository<NaverCommissionSnapshot, Long> {
	Optional<NaverCommissionSnapshot> findByConnectionIdAndBaseDateAndProductOrderNoAndCommissionType(
			Long connectionId,
			LocalDate baseDate,
			String productOrderNo,
			String commissionType
	);
}
