package com.sellerradar.smartstore.settlement.repository;

import com.sellerradar.smartstore.settlement.domain.NaverSettlementSnapshot;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NaverSettlementSnapshotRepository extends JpaRepository<NaverSettlementSnapshot, Long> {
	Optional<NaverSettlementSnapshot> findByConnectionIdAndSettlementDateAndProductOrderNo(
			Long connectionId,
			LocalDate settlementDate,
			String productOrderNo
	);
}
