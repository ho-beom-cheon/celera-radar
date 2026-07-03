package com.sellerradar.keyword.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.keyword.dto.KeywordCreateRequest;
import com.sellerradar.keyword.dto.KeywordUpdateRequest;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import com.sellerradar.shopping.domain.ShoppingTopItem;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import com.sellerradar.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

	@BeforeEach
	void setUp() {
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
	void createAndListKeywords() throws Exception {
		AuthResponse auth = signup();

		mockMvc.perform(post("/api/v1/keywords")
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new KeywordCreateRequest(" 차량용   수납함 ", CategoryCode.CAR_ACCESSORY, null)
						)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.keyword").value("차량용 수납함"))
				.andExpect(jsonPath("$.data.categoryCode").value(CategoryCode.CAR_ACCESSORY.name()))
				.andExpect(jsonPath("$.data.priority").value("MEDIUM"))
				.andExpect(jsonPath("$.data.analysisStatus").value(AnalysisStatus.PENDING.name()));

		mockMvc.perform(get("/api/v1/keywords")
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].keyword").value("차량용 수납함"))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.totalPages").value(1));

		mockMvc.perform(get("/api/v1/keywords")
						.param("status", AnalysisStatus.PENDING.name())
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].analysisStatus").value(AnalysisStatus.PENDING.name()));
	}

	@Test
	void createRejectsDuplicatedKeywordForSameUser() throws Exception {
		AuthResponse auth = signup();
		createKeyword(auth, "차량용 수납함", CategoryCode.CAR_ACCESSORY);

		mockMvc.perform(post("/api/v1/keywords")
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new KeywordCreateRequest(" 차량용  수납함 ", CategoryCode.HOME_STORAGE, null)
						)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.DUPLICATED_KEYWORD.name()))
				.andExpect(jsonPath("$.error.field").value("keyword"));
	}

	@Test
	void createRejectsFreePlanKeywordLimit() throws Exception {
		AuthResponse auth = signup();
		createKeyword(auth, "차량용 수납함", CategoryCode.CAR_ACCESSORY);
		createKeyword(auth, "케이블 정리함", CategoryCode.DESK_OFFICE);
		createKeyword(auth, "욕실 수납함", CategoryCode.BATH_CLEANING);

		mockMvc.perform(post("/api/v1/keywords")
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new KeywordCreateRequest("여행 파우치", CategoryCode.TRAVEL_ORGANIZER, null)
						)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.KEYWORD_LIMIT_EXCEEDED.name()))
				.andExpect(jsonPath("$.error.field").value("keyword"));
	}

	@Test
	void updateKeyword() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "차량용 수납함", CategoryCode.CAR_ACCESSORY);

		mockMvc.perform(put("/api/v1/keywords/{keywordId}", keywordId)
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new KeywordUpdateRequest("케이블 정리 트레이", CategoryCode.DESK_OFFICE, null)
						)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(keywordId))
				.andExpect(jsonPath("$.data.keyword").value("케이블 정리 트레이"))
				.andExpect(jsonPath("$.data.categoryCode").value(CategoryCode.DESK_OFFICE.name()))
				.andExpect(jsonPath("$.data.priority").value("MEDIUM"));
	}

	@Test
	void deleteKeywordSoftDeletesAndHidesFromList() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "차량용 수납함", CategoryCode.CAR_ACCESSORY);

		mockMvc.perform(delete("/api/v1/keywords/{keywordId}", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		assertThat(keywordRepository.findById(keywordId).orElseThrow().getStatus())
				.isEqualTo(KeywordStatus.DELETED);

		mockMvc.perform(get("/api/v1/keywords")
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0))
				.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	void analysisReturnsPendingStateWhenSnapshotDoesNotExist() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "차량용 수납함", CategoryCode.CAR_ACCESSORY);

		mockMvc.perform(get("/api/v1/keywords/{keywordId}/analysis", keywordId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.keywordId").value(keywordId))
				.andExpect(jsonPath("$.data.keyword").value("차량용 수납함"))
				.andExpect(jsonPath("$.data.status").value(AnalysisStatus.PENDING.name()))
				.andExpect(jsonPath("$.data.shopping").doesNotExist());
	}

	@Test
	void analysisReturnsLatestShoppingSnapshot() throws Exception {
		AuthResponse auth = signup();
		Long keywordId = createKeyword(auth, "차량용 수납함", CategoryCode.CAR_ACCESSORY);
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
				"차량용 먼지 제거 브러쉬",
				"https://example.com/item",
				"https://example.com/item.jpg",
				4900,
				5900,
				"sample mall",
				"1000001",
				"1",
				"sample brand",
				"sample maker",
				"생활/건강",
				"자동차용품",
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
				.andExpect(jsonPath("$.data.shopping.topItems[0].title").value("차량용 먼지 제거 브러쉬"))
				.andExpect(jsonPath("$.data.shopping.topItems[0].mallName").value("sample mall"));
	}

	private AuthResponse signup() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SignupRequest(EMAIL, PASSWORD, true))))
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

	private Long createKeyword(AuthResponse auth, String keyword, CategoryCode categoryCode) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/keywords")
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new KeywordCreateRequest(keyword, categoryCode, null))))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsByteArray())
				.get("data")
				.get("id")
				.asLong();
	}

	private String bearer(AuthResponse auth) {
		return "Bearer " + auth.accessToken();
	}
}
