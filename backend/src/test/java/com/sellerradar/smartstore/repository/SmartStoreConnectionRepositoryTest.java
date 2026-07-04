package com.sellerradar.smartstore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.smartstore.domain.SmartStoreConnection;
import com.sellerradar.smartstore.domain.SmartStoreConnectionStatus;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SmartStoreConnectionRepositoryTest {
	@Autowired
	private SmartStoreConnectionRepository connectionRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void saveConnectionStoresUserScopedStatus() {
		User user = userRepository.save(User.create("smartstore-owner@example.com", "{bcrypt}hash"));
		User otherUser = userRepository.save(User.create("smartstore-other@example.com", "{bcrypt}hash"));
		SmartStoreConnection connection = SmartStoreConnection.disconnected(
				user,
				"owner store",
				"store-001",
				"seller-001"
		);
		connection.markConnected(
				"encrypted-access-token",
				"encrypted-refresh-token",
				OffsetDateTime.parse("2026-07-04T03:00:00+09:00"),
				OffsetDateTime.parse("2026-07-04T02:00:00+09:00")
		);

		SmartStoreConnection saved = connectionRepository.saveAndFlush(connection);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getConnectionStatus()).isEqualTo(SmartStoreConnectionStatus.CONNECTED);
		assertThat(saved.getAccessTokenEncrypted()).isEqualTo("encrypted-access-token");
		assertThat(saved.getRefreshTokenEncrypted()).isEqualTo("encrypted-refresh-token");
		assertThat(connectionRepository.existsByUserIdAndStoreId(user.getId(), "store-001")).isTrue();
		assertThat(connectionRepository.existsByUserIdAndStoreId(otherUser.getId(), "store-001")).isFalse();
		assertThat(connectionRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())).isPresent();
		assertThat(connectionRepository.findFirstByUserIdOrderByCreatedAtDesc(otherUser.getId())).isEmpty();
	}
}
