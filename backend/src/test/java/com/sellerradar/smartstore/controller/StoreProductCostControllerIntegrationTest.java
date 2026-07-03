package com.sellerradar.smartstore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.smartstore.domain.SmartStoreConnection;
import com.sellerradar.smartstore.repository.SmartStoreConnectionRepository;
import com.sellerradar.smartstore.repository.SmartStoreProductRepository;
import com.sellerradar.smartstore.repository.SmartStoreProductSyncHistoryRepository;
import com.sellerradar.smartstore.repository.StoreProductCostRepository;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import com.sellerradar.wholesale.domain.CsvEncoding;
import com.sellerradar.wholesale.domain.WholesaleFile;
import com.sellerradar.wholesale.domain.WholesaleProduct;
import com.sellerradar.wholesale.repository.WholesaleFileRepository;
import com.sellerradar.wholesale.repository.WholesaleProductRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
class StoreProductCostControllerIntegrationTest {
	private static final String PASSWORD = "password1234";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StoreProductCostRepository costRepository;

	@Autowired
	private SmartStoreProductRepository productRepository;

	@Autowired
	private SmartStoreProductSyncHistoryRepository historyRepository;

	@Autowired
	private SmartStoreConnectionRepository connectionRepository;

	@Autowired
	private WholesaleProductRepository wholesaleProductRepository;

	@Autowired
	private WholesaleFileRepository wholesaleFileRepository;

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
		costRepository.deleteAll();
		historyRepository.deleteAll();
		productRepository.deleteAll();
		connectionRepository.deleteAll();
		wholesaleProductRepository.deleteAll();
		wholesaleFileRepository.deleteAll();
	}

	@Test
	void upsertAndGetManualCostMappingReturnsMarginCalculation() throws Exception {
		AuthResponse auth = signup("store-cost-manual@example.com");
		Long productId = syncProduct(auth);

		MvcResult result = mockMvc.perform(put("/api/v1/smartstore/products/{productId}/cost", productId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(costPayload(
								null,
								new BigDecimal("7000"),
								new BigDecimal("2500"),
								new BigDecimal("500"),
								new BigDecimal("300"),
								new BigDecimal("4.0"),
								new BigDecimal("25.0"),
								"manual cost"
						)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.storeProductId").value(productId))
				.andExpect(jsonPath("$.data.wholesaleProductId").doesNotExist())
				.andExpect(jsonPath("$.data.purchaseCost").value(7000))
				.andExpect(jsonPath("$.data.shippingFee").value(2500))
				.andExpect(jsonPath("$.data.packagingFee").value(500))
				.andExpect(jsonPath("$.data.extraCost").value(300))
				.andExpect(jsonPath("$.data.salePrice").value(12900))
				.andExpect(jsonPath("$.data.totalCost").value(10816))
				.andExpect(jsonPath("$.data.expectedProfit").value(2084))
				.andExpect(jsonPath("$.data.expectedMarginRate").value(16.16))
				.andExpect(jsonPath("$.data.recommendedSalePrice").value(14600))
				.andExpect(jsonPath("$.data.memo").value("manual cost"))
				.andReturn();

		Long costId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
				.get("data")
				.get("id")
				.asLong();

		mockMvc.perform(get("/api/v1/smartstore/products/{productId}/cost", productId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(costId))
				.andExpect(jsonPath("$.data.expectedMarginRate").value(16.16));
	}

	@Test
	void upsertCostMappingUsesWholesaleProductDefaultsAndUpdatesExistingMapping() throws Exception {
		AuthResponse auth = signup("store-cost-wholesale@example.com");
		Long productId = syncProduct(auth);
		WholesaleProduct wholesaleProduct = createWholesaleProduct(auth, "Supplier Desk", 5000, 2000);

		MvcResult firstResult = mockMvc.perform(put("/api/v1/smartstore/products/{productId}/cost", productId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(costPayload(
								wholesaleProduct.getId(),
								null,
								null,
								new BigDecimal("500"),
								null,
								null,
								new BigDecimal("25.0"),
								null
						)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.wholesaleProductId").value(wholesaleProduct.getId()))
				.andExpect(jsonPath("$.data.wholesaleProductName").value("Supplier Desk"))
				.andExpect(jsonPath("$.data.purchaseCost").value(5000))
				.andExpect(jsonPath("$.data.shippingFee").value(2000))
				.andExpect(jsonPath("$.data.packagingFee").value(500))
				.andExpect(jsonPath("$.data.expectedProfit").value(5400))
				.andReturn();

		Long costId = objectMapper.readTree(firstResult.getResponse().getContentAsByteArray())
				.get("data")
				.get("id")
				.asLong();

		mockMvc.perform(put("/api/v1/smartstore/products/{productId}/cost", productId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(costPayload(
								wholesaleProduct.getId(),
								new BigDecimal("5500"),
								new BigDecimal("1800"),
								new BigDecimal("300"),
								new BigDecimal("200"),
								null,
								new BigDecimal("25.0"),
								"updated"
						)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(costId))
				.andExpect(jsonPath("$.data.purchaseCost").value(5500))
				.andExpect(jsonPath("$.data.shippingFee").value(1800))
				.andExpect(jsonPath("$.data.memo").value("updated"));

		assertThat(costRepository.count()).isEqualTo(1);
	}

	@Test
	void costMappingIsIsolatedByAuthenticatedUser() throws Exception {
		AuthResponse ownerAuth = signup("store-cost-owner@example.com");
		AuthResponse otherAuth = signup("store-cost-other@example.com");
		Long productId = syncProduct(ownerAuth);
		WholesaleProduct otherWholesaleProduct = createWholesaleProduct(otherAuth, "Other Supplier", 1000, 500);

		mockMvc.perform(put("/api/v1/smartstore/products/{productId}/cost", productId)
						.header("Authorization", bearer(otherAuth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(costPayload(
								null,
								new BigDecimal("7000"),
								null,
								null,
								null,
								null,
								null,
								null
						)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.SMARTSTORE_PRODUCT_NOT_FOUND.name()));

		mockMvc.perform(put("/api/v1/smartstore/products/{productId}/cost", productId)
						.header("Authorization", bearer(ownerAuth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(costPayload(
								otherWholesaleProduct.getId(),
								null,
								null,
								null,
								null,
								null,
								null,
								null
						)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.WHOLESALE_PRODUCT_NOT_FOUND.name()));
	}

	@Test
	void upsertCostMappingRequiresManualPurchaseCostWhenWholesaleProductIsMissing() throws Exception {
		AuthResponse auth = signup("store-cost-validation@example.com");
		Long productId = syncProduct(auth);

		mockMvc.perform(put("/api/v1/smartstore/products/{productId}/cost", productId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(costPayload(
								null,
								null,
								null,
								null,
								null,
								null,
								null,
								null
						)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REQUEST.name()))
				.andExpect(jsonPath("$.error.field").value("purchaseCost"));
	}

	private Long syncProduct(AuthResponse auth) throws Exception {
		Long connectionId = createConnection(auth);
		mockMvc.perform(post("/api/v1/smartstore/connections/{connectionId}/products/sync", connectionId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk());
		return productRepository.findByUserIdOrderByLastSyncedAtDesc(
						auth.userId(),
						org.springframework.data.domain.PageRequest.of(0, 1)
				)
				.getContent()
				.getFirst()
				.getId();
	}

	private Long createConnection(AuthResponse auth) {
		User user = userRepository.findById(auth.userId()).orElseThrow();
		return connectionRepository.saveAndFlush(SmartStoreConnection.disconnected(
						user,
						"cost store " + auth.userId(),
						"cost-store-" + auth.userId(),
						"seller-cost-" + auth.userId()
				))
				.getId();
	}

	private WholesaleProduct createWholesaleProduct(
			AuthResponse auth,
			String productName,
			int supplyPrice,
			int shippingFee
	) {
		User user = userRepository.findById(auth.userId()).orElseThrow();
		WholesaleFile file = wholesaleFileRepository.saveAndFlush(WholesaleFile.uploaded(
				user,
				"supplier",
				"items-" + auth.userId() + ".csv",
				"/tmp/items-" + auth.userId() + ".csv",
				100L,
				CsvEncoding.UTF_8,
				CsvEncoding.UTF_8,
				1,
				List.of("productName", "supplyPrice", "shippingFee")
		));
		return wholesaleProductRepository.saveAndFlush(WholesaleProduct.parsed(
				file,
				2,
				productName,
				productName.toLowerCase(),
				supplyPrice,
				shippingFee,
				"Desk",
				null,
				null
		));
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

	private byte[] costPayload(
			Long wholesaleProductId,
			BigDecimal purchaseCost,
			BigDecimal shippingFee,
			BigDecimal packagingFee,
			BigDecimal extraCost,
			BigDecimal platformFeeRate,
			BigDecimal targetMarginRate,
			String memo
	) throws Exception {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("wholesaleProductId", wholesaleProductId);
		payload.put("purchaseCost", purchaseCost);
		payload.put("shippingFee", shippingFee);
		payload.put("packagingFee", packagingFee);
		payload.put("extraCost", extraCost);
		payload.put("platformFeeRate", platformFeeRate);
		payload.put("targetMarginRate", targetMarginRate);
		payload.put("memo", memo);
		return objectMapper.writeValueAsBytes(payload);
	}

	private String bearer(AuthResponse auth) {
		return "Bearer " + auth.accessToken();
	}
}
