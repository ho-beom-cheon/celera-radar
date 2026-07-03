package com.sellerradar.alert.repository;

import com.sellerradar.alert.domain.AlertRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
	List<AlertRule> findByActiveTrue();
}
