package com.sellerradar.smartstore.repository;

import com.sellerradar.smartstore.domain.StoreProductCost;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreProductCostRepository extends JpaRepository<StoreProductCost, Long> {
	Optional<StoreProductCost> findByStoreProductIdAndUserId(Long storeProductId, Long userId);
}
