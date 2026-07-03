package com.sellerradar.shopping.repository;

import com.sellerradar.shopping.domain.ShoppingTopItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingTopItemRepository extends JpaRepository<ShoppingTopItem, Long> {
}
