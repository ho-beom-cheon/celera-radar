package com.sellerradar.smartstore.repository;

import com.sellerradar.smartstore.domain.SmartStoreConnection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartStoreConnectionRepository extends JpaRepository<SmartStoreConnection, Long> {
	Optional<SmartStoreConnection> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

	List<SmartStoreConnection> findByUserIdOrderByCreatedAtDesc(Long userId);

	boolean existsByUserIdAndStoreId(Long userId, String storeId);
}
