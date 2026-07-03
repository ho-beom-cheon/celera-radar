package com.sellerradar.wholesale.repository;

import com.sellerradar.wholesale.domain.WholesaleFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WholesaleFileRepository extends JpaRepository<WholesaleFile, Long> {
	Optional<WholesaleFile> findByIdAndUserId(Long id, Long userId);
}
