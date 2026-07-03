package com.sellerradar.smartstore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.batch.domain.BatchJobStatus;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.smartstore.domain.SmartStoreConnection;
import com.sellerradar.smartstore.repository.SmartStoreConnectionRepository;
import com.sellerradar.smartstore.repository.SmartStoreProductRepository;
import com.sellerradar.smartstore.repository.SmartStoreProductSyncHistoryRepository;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class SmartStoreProductControllerIntegrationTest {
	private static final String PASSWORD = "password1234";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private SmartStoreProductRepository productRepository;

	@Autowired
	private SmartStoreProductSyncHistoryRepository historyRepository;

	@Autowired
	private SmartStoreConnectionRepository connectionRepository;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		cleanDatabase();
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	private void cleanDatabase() {
		historyRepository.deleteAll();
		productRepository.deleteAll();
		connectionRepository.deleteAll();
		keywordRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void syncProductsStoresMockAdapterProductsAndHistory() throws Exception {
		AuthResponse auth = signup("smartstore-sync@example.com");
		Long connectionId = createConnection(auth, "sync store", "sync-store-001", "seller-sync-001");

		mockMvc.perform(post("/api/v1/smartstore/connections/{connectionId}/products/sync", connectionId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.connectionId").value(connectionId))
				.andExpect(jsonPath("$.data.status").value(BatchJobStatus.SUCCESS.name()))
				.andExpect(jsonPath("$.data.targetCount").value(1))
				.andExpect(jsonPath("$.data.successCount").value(1))
				.andExpect(jsonPath("$.data.failureCount").value(0));

		assertThat(productRepository.countByConnectionId(connectionId)).isEqualTo(1);
		assertThat(historyRepository.findAll()).hasSize(1);

		mockMvc.perform(get("/api/v1/smartstore/products")
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].sourceProductId").value("mock-product-" + connectionId))
				.andExpect(jsonPath("$.data.items[0].productName").value("Mock SmartStore Product"))
				.andExpect(jsonPath("$.data.items[0].saleStatus").value("SALE"));
	}

	@Test
	void syncProductsUpsertsSameSourceProduct() throws Exception {
		AuthResponse auth = signup("smartstore-upsert@example.com");
		Long connectionId = createConnection(auth, "upsert store", "upsert-store-001", "seller-upsert-001");

		mockMvc.perform(post("/api/v1/smartstore/connections/{connectionId}/products/sync", connectionId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/smartstore/connections/{connectionId}/products/sync", connectionId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk());

		assertThat(productRepository.countByConnectionId(connectionId)).isEqualTo(1);
		assertThat(historyRepository.findAll()).hasSize(2);
	}

	@Test
	void syncProductsDoesNotExposeOtherUsersConnectionOrProducts() throws Exception {
		AuthResponse ownerAuth = signup("smartstore-owner@example.com");
		AuthResponse otherAuth = signup("smartstore-other@example.com");
		Long connectionId = createConnection(ownerAuth, "owner store", "owner-store-001", "seller-owner-001");

		mockMvc.perform(post("/api/v1/smartstore/connections/{connectionId}/products/sync", connectionId)
						.header("Authorization", bearer(ownerAuth)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/smartstore/products")
						.header("Authorization", bearer(otherAuth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0))
				.andExpect(jsonPath("$.data.totalElements").value(0));

		mockMvc.perform(post("/api/v1/smartstore/connections/{connectionId}/products/sync", connectionId)
						.header("Authorization", bearer(otherAuth)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.SMARTSTORE_CONNECTION_NOT_FOUND.name()));
	}

	private Long createConnection(AuthResponse auth, String storeName, String storeId, String sellerId) {
		User user = userRepository.findById(auth.userId()).orElseThrow();
		return connectionRepository.saveAndFlush(SmartStoreConnection.disconnected(
						user,
						storeName,
						storeId,
						sellerId
				))
				.getId();
	}

	private AuthResponse signup(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SignupRequest(email, PASSWORD, true))))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode data = objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
		return new AuthResponse(
				data.get("userId").asLong(),
				data.get("email").asText(),
				null,
				data.get("accessToken").asText(),
				data.get("refreshToken").asText()
		);
	}

	private String bearer(AuthResponse auth) {
		return "Bearer " + auth.accessToken();
	}
}
