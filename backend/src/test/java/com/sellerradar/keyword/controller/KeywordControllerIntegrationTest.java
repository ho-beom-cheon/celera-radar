package com.sellerradar.keyword.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.repository.ApiCallLogRepository;
import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.client.NaverShoppingClient;
import com.sellerradar.shopping.client.NaverShoppingSearchItem;
import com.sellerradar.shopping.client.NaverShoppingSearchRequest;
import com.sellerradar.shopping.client.NaverShoppingSearchResponse;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import com.sellerradar.shopping.domain.ShoppingTopItem;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import com.sellerradar.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class KeywordControllerIntegrationTest {
	private static final String EMAIL = "keyword-owner@example.com";
	private static final String PASSWORD = "password1234";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private ShoppingPriceSnapshotRepository snapshotRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ApiCallLogRepository apiCallLogRepository;

	@MockitoBean
	private NaverShoppingClient naverShoppingClient;

	@BeforeEach
	void setUp() {
		apiCallLogRepository.deleteAll();
		snapshotRepository.deleteAll();
		keywordRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void keywordEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/keywords"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.AUTH_REQUIRED.name()));
	}

	@Test
	void createListAndDetailKeywordsUseStringCategory() throws Exception {
		AuthResponse auth = signup();

		MvcResult result = mockMvc.perform(post("/api/v1/keywords")
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(keywordPayload(" car   storage box ", " Car Gear ")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.keyword").value("car storage box"))
				.andExpect(jsonPath("$.data.category").value("Car Gear"))
				.andExpect(jsonPath("$.data.active").value(true))
				.andExpect(jsonPath("$.data.analysisStatus").value(AnalysisStatus.PENDING.name()))
				.andReturn();
		Long keywordId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
				.get("data")
				.get("id")
				.asLong();

		mockMvc.perform(get("/api/v1/keywords")
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].keyword").value("car storage box"))
				.andExpect(jsonPath("$.data.items[0].category").value("Car Gear"))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.totalPages").value(1));

		mockMvc.perform(get("/api/v1/keywords")
						.param("analysisStatus", AnalysisStatus.PENDING.name())
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].analysisStatus").value(AnalysisStatus.PENDING.name()));

		mockMvc.perform(get("/api/v1/keywords/{keywordId}", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(keywordId))
				.andExpect(jsonPath("$.data.keyword").value("car storage box"))
				.andExpect(jsonPath("$.data.category").value("Car Gear"));
	}

	@Test
	void createStoresBlankCategoryAsNull() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "car storage box", "   ");

		assertThat(keywordRepository.findById(keywordId).orElseThrow().getCategory()).isNull();
	}

	@Test
	void createRejectsDuplicatedKeywordForSameUserOnly() throws Exception {
		AuthResponse auth = signup();
		createKeyword(auth, "car storage box", "Car Gear");

		mockMvc.perform(post("/api/v1/keywords")
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(keywordPayload(" car  storage box ", "Storage")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.DUPLICATED_KEYWORD.name()))
				.andExpect(jsonPath("$.error.field").value("keyword"));

		AuthResponse otherAuth = signup("other-keyword-owner@example.com");
		mockMvc.perform(post("/api/v1/keywords")
						.header("Authorization", bearer(otherAuth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(keywordPayload("car storage box", "Car Gear")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	void createRejectsFreePlanKeywordLimit() throws Exception {
		AuthResponse auth = signup();
		createKeyword(auth, "car storage box", "Car Gear");
		createKeyword(auth, "desk cable tray", "Desk");
		createKeyword(auth, "bath storage rack", "Bath");

		mockMvc.perform(post("/api/v1/keywords")
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(keywordPayload("travel pouch", "Travel")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.KEYWORD_LIMIT_EXCEEDED.name()))
				.andExpect(jsonPath("$.error.field").value("keyword"));
	}

	@Test
	void listRejectsInvalidAnalysisStatus() throws Exception {
		AuthResponse auth = signup();

		mockMvc.perform(get("/api/v1/keywords")
						.param("analysisStatus", "ANALYZED")
						.header("Authorization", bearer(auth)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REQUEST.name()));
	}

	@Test
	void updateKeywordAndPreventDuplicateNormalizedKeyword() throws Exception {
		AuthResponse auth = signup();
		Long firstKeywordId = createKeyword(auth, "car storage box", "Car Gear");
		Long secondKeywordId = createKeyword(auth, "desk cable tray", "Desk");

		mockMvc.perform(put("/api/v1/keywords/{keywordId}", firstKeywordId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(keywordPayload("car storage box updated", "Updated Category")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(firstKeywordId))
				.andExpect(jsonPath("$.data.keyword").value("car storage box updated"))
				.andExpect(jsonPath("$.data.category").value("Updated Category"));

		mockMvc.perform(put("/api/v1/keywords/{keywordId}", secondKeywordId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(keywordPayload(" car storage box updated ", "Desk")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.DUPLICATED_KEYWORD.name()));
	}

	@Test
	void deleteKeywordSoftDeletesHidesFromListAndAllowsReRegistration() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "car storage box", "Car Gear");

		mockMvc.perform(delete("/api/v1/keywords/{keywordId}", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		var deletedKeyword = keywordRepository.findById(keywordId).orElseThrow();
		assertThat(deletedKeyword.getStatus()).isEqualTo(KeywordStatus.DELETED);
		assertThat(deletedKeyword.isActive()).isFalse();
		assertThat(deletedKeyword.getDeletedAt()).isNotNull();

		mockMvc.perform(get("/api/v1/keywords")
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0))
				.andExpect(jsonPath("$.data.totalElements").value(0));

		mockMvc.perform(get("/api/v1/keywords/{keywordId}", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.KEYWORD_NOT_FOUND.name()));

		mockMvc.perform(post("/api/v1/keywords")
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(keywordPayload(" car storage box ", "Car Gear")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.keyword").value("car storage box"));
	}

	@Test
	void analysisReturnsPendingStateWhenSnapshotDoesNotExist() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "car storage box", "Car Gear");

		mockMvc.perform(get("/api/v1/keywords/{keywordId}/analysis", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.keywordId").value(keywordId))
				.andExpect(jsonPath("$.data.keyword").value("car storage box"))
				.andExpect(jsonPath("$.data.status").value(AnalysisStatus.PENDING.name()))
				.andExpect(jsonPath("$.data.shopping").doesNotExist());
	}

	@Test
	void analysisReturnsLatestShoppingSnapshot() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "car storage box", "Car Gear");
		var keyword = keywordRepository.findById(keywordId).orElseThrow();
		keyword.markAnalyzed(OffsetDateTime.parse("2026-07-02T07:12:00+09:00"));
		keywordRepository.saveAndFlush(keyword);
		ShoppingPriceSnapshot snapshot = ShoppingPriceSnapshot.create(
				keyword,
				LocalDate.of(2026, 7, 2),
				18230L,
				4900,
				29900,
				12300,
				"{}"
		);
		snapshot.addTopItem(ShoppingTopItem.create(
				1,
				"car dust brush",
				"https://example.com/item",
				"https://example.com/item.jpg",
				4900,
				5900,
				"sample mall",
				"1000001",
				"1",
				"sample brand",
				"sample maker",
				"living",
				"car supplies",
				"",
				""
		));
		snapshotRepository.saveAndFlush(snapshot);

		mockMvc.perform(get("/api/v1/keywords/{keywordId}/analysis", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(AnalysisStatus.SUCCESS.name()))
				.andExpect(jsonPath("$.data.shopping.baseDate").value("2026-07-02"))
				.andExpect(jsonPath("$.data.shopping.totalResults").value(18230))
				.andExpect(jsonPath("$.data.shopping.minPrice").value(4900))
				.andExpect(jsonPath("$.data.shopping.maxPrice").value(29900))
				.andExpect(jsonPath("$.data.shopping.avgPrice").value(12300))
				.andExpect(jsonPath("$.data.shopping.topItems.length()").value(1))
				.andExpect(jsonPath("$.data.shopping.topItems[0].title").value("car dust brush"))
				.andExpect(jsonPath("$.data.shopping.topItems[0].mallName").value("sample mall"));
	}

	@Test
	void analyzeShoppingStoresSnapshotAndReusesSameDateCache() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "car storage box", "Car Gear");
		when(naverShoppingClient.search(any(NaverShoppingSearchRequest.class))).thenReturn(shoppingResponse(11));

		mockMvc.perform(post("/api/v1/keywords/{keywordId}/analyze/shopping", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.keywordId").value(keywordId))
				.andExpect(jsonPath("$.data.keyword").value("car storage box"))
				.andExpect(jsonPath("$.data.cached").value(false))
				.andExpect(jsonPath("$.data.sortType").value("sim"))
				.andExpect(jsonPath("$.data.totalCount").value(1234))
				.andExpect(jsonPath("$.data.minPrice").value(1000))
				.andExpect(jsonPath("$.data.maxPrice").value(11000))
				.andExpect(jsonPath("$.data.avgPrice").value(6000))
				.andExpect(jsonPath("$.data.fetchedAt").exists())
				.andExpect(jsonPath("$.data.topItems.length()").value(10))
				.andExpect(jsonPath("$.data.topItems[0].rankNo").value(1))
				.andExpect(jsonPath("$.data.topItems[0].title").value("Product 1"))
				.andExpect(jsonPath("$.data.topItems[0].productUrl").value("https://example.com/products/1"))
				.andExpect(jsonPath("$.data.topItems[0].imageUrl").value("https://example.com/products/1.jpg"))
				.andExpect(jsonPath("$.data.topItems[0].lowPrice").value(1000))
				.andExpect(jsonPath("$.data.topItems[0].mallName").value("Mall 1"));

		mockMvc.perform(post("/api/v1/keywords/{keywordId}/analyze/shopping", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.cached").value(true))
				.andExpect(jsonPath("$.data.topItems.length()").value(10));

		verify(naverShoppingClient, times(1)).search(any(NaverShoppingSearchRequest.class));
		assertThat(snapshotRepository.count()).isEqualTo(1);
		assertThat(apiCallLogRepository.count()).isEqualTo(1);
		var keyword = keywordRepository.findById(keywordId).orElseThrow();
		assertThat(keyword.getAnalysisStatus()).isEqualTo(AnalysisStatus.SUCCESS);
		assertThat(keyword.getLastAnalyzedAt()).isNotNull();
		assertThat(keyword.getLastSnapshotDate()).isNotNull();
	}

	@Test
	void latestShoppingSnapshotReturnsAcceptedWhenSnapshotDoesNotExist() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "car storage box", "Car Gear");

		mockMvc.perform(get("/api/v1/keywords/{keywordId}/shopping-snapshot/latest", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.ANALYSIS_NOT_READY.name()));
	}

	@Test
	void latestShoppingSnapshotReturnsStoredSummaryAndTopItems() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "car storage box", "Car Gear");
		var keyword = keywordRepository.findById(keywordId).orElseThrow();
		ShoppingPriceSnapshot snapshot = ShoppingPriceSnapshot.create(
				keyword,
				LocalDate.of(2026, 7, 2),
				18230L,
				4900,
				29900,
				12300,
				"{}"
		);
		snapshot.addTopItem(ShoppingTopItem.create(
				1,
				"<b>car dust brush</b>",
				"https://example.com/item",
				"https://example.com/item.jpg",
				4900,
				5900,
				"sample mall",
				"1000001",
				"1",
				"sample brand",
				"sample maker",
				"living",
				"car supplies",
				"",
				""
		));
		snapshotRepository.saveAndFlush(snapshot);

		mockMvc.perform(get("/api/v1/keywords/{keywordId}/shopping-snapshot/latest", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.keywordId").value(keywordId))
				.andExpect(jsonPath("$.data.searchDate").value("2026-07-02"))
				.andExpect(jsonPath("$.data.cached").value(false))
				.andExpect(jsonPath("$.data.totalCount").value(18230))
				.andExpect(jsonPath("$.data.minPrice").value(4900))
				.andExpect(jsonPath("$.data.maxPrice").value(29900))
				.andExpect(jsonPath("$.data.avgPrice").value(12300))
				.andExpect(jsonPath("$.data.topItems.length()").value(1))
				.andExpect(jsonPath("$.data.topItems[0].rankNo").value(1))
				.andExpect(jsonPath("$.data.topItems[0].title").value("car dust brush"))
				.andExpect(jsonPath("$.data.topItems[0].productUrl").value("https://example.com/item"))
				.andExpect(jsonPath("$.data.topItems[0].imageUrl").value("https://example.com/item.jpg"))
				.andExpect(jsonPath("$.data.topItems[0].lowPrice").value(4900))
				.andExpect(jsonPath("$.data.topItems[0].mallName").value("sample mall"));
	}

	private AuthResponse signup() throws Exception {
		return signup(EMAIL);
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

	private Long createKeyword(AuthResponse auth, String keyword, String category) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/keywords")
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(keywordPayload(keyword, category)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsByteArray())
				.get("data")
				.get("id")
				.asLong();
	}

	private byte[] keywordPayload(String keyword, String category) throws Exception {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("keyword", keyword);
		payload.put("category", category);
		return objectMapper.writeValueAsBytes(payload);
	}

	private NaverShoppingSearchResponse shoppingResponse(int itemCount) {
		return new NaverShoppingSearchResponse(
				"Thu, 02 Jul 2026 20:00:00 +0900",
				1234L,
				1,
				itemCount,
				java.util.stream.IntStream.rangeClosed(1, itemCount)
						.mapToObj(index -> new NaverShoppingSearchItem(
								"<b>Product " + index + "</b>",
								"https://example.com/products/" + index,
								"https://example.com/products/" + index + ".jpg",
								String.valueOf(index * 1000),
								String.valueOf(index * 1000 + 500),
								"Mall " + index,
								"10000" + index,
								"1",
								"Brand " + index,
								"Maker " + index,
								"Category1",
								"Category2",
								"Category3",
								"Category4"
						))
						.toList()
		);
	}

	private String bearer(AuthResponse auth) {
		return "Bearer " + auth.accessToken();
	}
}
