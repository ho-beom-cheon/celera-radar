package com.sellerradar.smartstore.repository;

import com.sellerradar.smartstore.domain.SmartStoreProduct;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartStoreProductRepository extends JpaRepository<SmartStoreProduct, Long> {
	Optional<SmartStoreProduct> findByConnectionIdAndSourceProductId(Long connectionId, String sourceProductId);

	Page<SmartStoreProduct> findByUserIdOrderByLastSyncedAtDesc(Long userId, Pageable pageable);

	long countByConnectionId(Long connectionId);
}
