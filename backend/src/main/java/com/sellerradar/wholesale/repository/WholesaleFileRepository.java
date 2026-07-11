package com.sellerradar.wholesale.repository;

import com.sellerradar.wholesale.domain.WholesaleFile;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WholesaleFileRepository extends JpaRepository<WholesaleFile, Long> {
	Optional<WholesaleFile> findByIdAndUserId(Long id, Long userId);

	List<WholesaleFile> findByRawDeletedAtIsNullAndRawExpiresAtLessThanEqualOrderByRawExpiresAtAsc(
			OffsetDateTime now,
			Pageable pageable
	);
}
