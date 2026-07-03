package com.sellerradar.smartstore.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.smartstore.client.MockSmartStoreClient;
import com.sellerradar.smartstore.client.SmartStoreConnectionProfile;
import com.sellerradar.smartstore.domain.SmartStoreConnection;
import com.sellerradar.smartstore.domain.SmartStoreConnectionStatus;
import com.sellerradar.smartstore.dto.SmartStoreConnectionStatusResponse;
import com.sellerradar.smartstore.repository.SmartStoreConnectionRepository;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SmartStoreConnectionServiceTest {
	@Autowired
	private SmartStoreConnectionService connectionService;

	@Autowired
	private SmartStoreConnectionRepository connectionRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void getLatestStatusReturnsDisconnectedWhenConnectionDoesNotExist() {
		User user = userRepository.save(User.create("smartstore-empty@example.com", "{bcrypt}hash"));

		SmartStoreConnectionStatusResponse response = connectionService.getLatestStatus(user.getId());

		assertThat(response.connectionId()).isNull();
		assertThat(response.status()).isEqualTo(SmartStoreConnectionStatus.DISCONNECTED);
	}

	@Test
	void registerDisconnectedStoresConnectionForUser() {
		User user = userRepository.save(User.create("smartstore-register@example.com", "{bcrypt}hash"));

		SmartStoreConnectionStatusResponse response = connectionService.registerDisconnected(
				user.getId(),
				"sample store",
				"store-100",
				"seller-100"
		);

		assertThat(response.connectionId()).isNotNull();
		assertThat(response.storeName()).isEqualTo("sample store");
		assertThat(response.status()).isEqualTo(SmartStoreConnectionStatus.DISCONNECTED);
		assertThat(connectionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).hasSize(1);
	}

	@Test
	void mockClientReturnsConnectionProfileWithoutExternalCall() {
		User user = userRepository.save(User.create("smartstore-mock@example.com", "{bcrypt}hash"));
		SmartStoreConnection connection = connectionRepository.saveAndFlush(SmartStoreConnection.disconnected(
				user,
				"mock store",
				"store-mock",
				"seller-mock"
		));
		MockSmartStoreClient client = new MockSmartStoreClient();

		SmartStoreConnectionProfile profile = client.fetchConnectionProfile(connection);

		assertThat(profile.connectionId()).isEqualTo(connection.getId());
		assertThat(profile.storeId()).isEqualTo("store-mock");
		assertThat(profile.status()).isEqualTo(SmartStoreConnectionStatus.DISCONNECTED);
	}
}
