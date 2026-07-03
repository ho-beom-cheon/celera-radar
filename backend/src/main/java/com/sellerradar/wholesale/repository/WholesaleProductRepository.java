package com.sellerradar.wholesale.repository;

import com.sellerradar.wholesale.domain.WholesaleFile;
import com.sellerradar.wholesale.domain.WholesaleProduct;
import com.sellerradar.wholesale.domain.WholesaleProductParseStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WholesaleProductRepository extends JpaRepository<WholesaleProduct, Long> {
	void deleteByFile(WholesaleFile file);

	Page<WholesaleProduct> findByFileIdOrderByRowNoAsc(Long fileId, Pageable pageable);

	List<WholesaleProduct> findByFileIdAndParseStatusOrderByRowNoAsc(Long fileId, WholesaleProductParseStatus parseStatus);
}
