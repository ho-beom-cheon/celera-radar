package com.sellerradar.smartstore.repository;

import com.sellerradar.smartstore.domain.SmartStoreProductSyncHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartStoreProductSyncHistoryRepository extends JpaRepository<SmartStoreProductSyncHistory, Long> {
	Page<SmartStoreProductSyncHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
