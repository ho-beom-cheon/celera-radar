package com.sellerradar.category.repository;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.category.domain.CategoryMaster;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryMasterRepository extends JpaRepository<CategoryMaster, CategoryCode> {
	List<CategoryMaster> findByActiveTrueOrderBySortOrderAsc();
}
