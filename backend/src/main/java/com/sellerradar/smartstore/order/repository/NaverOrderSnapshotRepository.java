package com.sellerradar.smartstore.order.repository;

import com.sellerradar.smartstore.order.domain.NaverOrderSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NaverOrderSnapshotRepository extends JpaRepository<NaverOrderSnapshot, Long> {
	Optional<NaverOrderSnapshot> findByConnectionIdAndProductOrderNo(Long connectionId, String productOrderNo);
}
