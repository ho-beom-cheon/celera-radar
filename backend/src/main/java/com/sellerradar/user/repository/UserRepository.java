package com.sellerradar.user.repository;

import com.sellerradar.user.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
	boolean existsByEmailAndDeletedAtIsNull(String email);

	Optional<User> findByEmailAndDeletedAtIsNull(String email);

	Optional<User> findByPublicIdAndDeletedAtIsNull(UUID publicId);

	@Query("SELECT user FROM User user WHERE user.id = :userId AND user.active = true AND user.deletedAt IS NULL")
	Optional<User> findActiveById(@Param("userId") Long userId);

	default boolean existsByEmail(String email) {
		return existsByEmailAndDeletedAtIsNull(email);
	}

	default Optional<User> findByEmail(String email) {
		return findByEmailAndDeletedAtIsNull(email);
	}
}
