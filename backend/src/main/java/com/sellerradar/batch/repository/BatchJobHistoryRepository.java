package com.sellerradar.batch.repository;

import com.sellerradar.batch.domain.BatchJobHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchJobHistoryRepository extends JpaRepository<BatchJobHistory, Long> {
	Page<BatchJobHistory> findAllByOrderByStartedAtDesc(Pageable pageable);
}
