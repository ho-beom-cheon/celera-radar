package com.sellerradar.auth.session;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT session FROM AuthSession session JOIN FETCH session.user WHERE session.tokenHash = :tokenHash")
	Optional<AuthSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	@Modifying(flushAutomatically = true)
	@Query("""
			UPDATE AuthSession session
			SET session.revokedAt = :revokedAt
			WHERE session.familyId = :familyId
			  AND session.revokedAt IS NULL
			""")
	int revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") OffsetDateTime revokedAt);

	@Modifying(flushAutomatically = true)
	@Query("""
			UPDATE AuthSession session
			SET session.revokedAt = :revokedAt
			WHERE session.user.id = :userId
			  AND session.revokedAt IS NULL
			""")
	int revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") OffsetDateTime revokedAt);

	Optional<AuthSession> findByTokenHash(String tokenHash);
}
