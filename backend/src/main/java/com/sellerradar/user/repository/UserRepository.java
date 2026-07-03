package com.sellerradar.user.repository;

import com.sellerradar.user.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
	boolean existsByEmailAndDeletedAtIsNull(String email);

	Optional<User> findByEmailAndDeletedAtIsNull(String email);

	Optional<User> findByPublicIdAndDeletedAtIsNull(UUID publicId);

	default boolean existsByEmail(String email) {
		return existsByEmailAndDeletedAtIsNull(email);
	}

	default Optional<User> findByEmail(String email) {
		return findByEmailAndDeletedAtIsNull(email);
	}
}
