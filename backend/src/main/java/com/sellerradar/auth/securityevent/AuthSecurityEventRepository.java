package com.sellerradar.auth.securityevent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSecurityEventRepository extends JpaRepository<AuthSecurityEvent, Long> {
}
