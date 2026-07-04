package com.sellerradar.candidate.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.candidate.domain.CandidateScore;
import com.sellerradar.candidate.domain.CandidateSourceType;
import com.sellerradar.candidate.domain.CandidateStatus;
import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.domain.RiskLevel;
import com.sellerradar.candidate.repository.CandidateScoreRepository;
import com.sellerradar.candidate.repository.ProductCandidateRepository;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.scoring.CandidateGrade;
import com.sellerradar.scoring.ScoringBreakdown;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
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
class CandidateControllerIntegrationTest {
	private static final String PASSWORD = "password1234";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProductCandidateRepository candidateRepository;

	@Autowired
	private CandidateScoreRepository candidateScoreRepository;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		candidateScoreRepository.deleteAll();
		candidateRepository.deleteAll();
		keywordRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void candidateEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/candidates"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.AUTH_REQUIRED.name()));
	}

	@Test
	void listCandidatesAppliesFiltersAndHidesExcludedCandidates() throws Exception {
		AuthResponse auth = signup("candidate-owner@example.com");
		User user = userRepository.findById(auth.userId()).orElseThrow();
		ProductCandidate target = createCandidate(
				user,
				"car dust brush",
				CandidateSourceType.CSV,
				CategoryCode.CAR_ACCESSORY,
				CandidateGrade.RECOMMENDED,
				82,
				new BigDecimal("31.20"),
				RiskLevel.LOW
		);
		createCandidate(
				user,
				"desk cable tray",
				CandidateSourceType.KEYWORD,
				CategoryCode.DESK_OFFICE,
				CandidateGrade.REVIEW,
				70,
				new BigDecimal("18.00"),
				RiskLevel.CAUTION
		);
		ProductCandidate excluded = createCandidate(
				user,
				"hidden item",
				CandidateSourceType.CSV,
				CategoryCode.CAR_ACCESSORY,
				CandidateGrade.RECOMMENDED,
				90,
				new BigDecimal("40.00"),
				RiskLevel.LOW
		);
		excluded.exclude();
		candidateRepository.saveAndFlush(excluded);

		mockMvc.perform(get("/api/v1/candidates")
						.header("Authorization", bearer(auth))
						.param("grade", CandidateGrade.RECOMMENDED.name())
						.param("categoryCode", CategoryCode.CAR_ACCESSORY.name())
						.param("minScore", "80")
						.param("minMarginRate", "20")
						.param("source", CandidateSourceType.CSV.name()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].candidateId").value(target.getId()))
				.andExpect(jsonPath("$.data.items[0].name").value("car dust brush"))
				.andExpect(jsonPath("$.data.items[0].score").value(82))
				.andExpect(jsonPath("$.data.items[0].riskLevel").value(RiskLevel.LOW.name()))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void getCandidateReturnsScoreBreakdownReasonsAndWarnings() throws Exception {
		AuthResponse auth = signup("candidate-detail@example.com");
		User user = userRepository.findById(auth.userId()).orElseThrow();
		ProductCandidate candidate = createCandidate(
				user,
				"storage basket",
				CandidateSourceType.CSV,
				CategoryCode.HOME_STORAGE,
				CandidateGrade.REVIEW,
				72,
				new BigDecimal("24.50"),
				RiskLevel.LOW
		);

		mockMvc.perform(get("/api/v1/candidates/{candidateId}", candidate.getId())
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.candidateId").value(candidate.getId()))
				.andExpect(jsonPath("$.data.status").value(CandidateStatus.ACTIVE.name()))
				.andExpect(jsonPath("$.data.scoreBreakdown.trendScore").value(20))
				.andExpect(jsonPath("$.data.scoreBreakdown.priceBandScore").value(7))
				.andExpect(jsonPath("$.data.scoreBreakdown.priceScore").value(7))
				.andExpect(jsonPath("$.data.scoreBreakdown.riskPenalty").value(0))
				.andExpect(jsonPath("$.data.reasons[0]").value("steady trend"))
				.andExpect(jsonPath("$.data.warnings[0]").value("review competition before sourcing"));
	}

	@Test
	void saveAndExcludeCandidateUpdatesStatus() throws Exception {
		AuthResponse auth = signup("candidate-status@example.com");
		User user = userRepository.findById(auth.userId()).orElseThrow();
		ProductCandidate candidate = createCandidate(
				user,
				"camping pouch",
				CandidateSourceType.CSV,
				CategoryCode.CAMPING_PICNIC,
				CandidateGrade.REVIEW,
				68,
				new BigDecimal("21.00"),
				RiskLevel.CAUTION
		);

		mockMvc.perform(post("/api/v1/candidates/{candidateId}/save", candidate.getId())
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(CandidateStatus.SAVED.name()));
		assertThat(candidateRepository.findById(candidate.getId()).orElseThrow().getStatus())
				.isEqualTo(CandidateStatus.SAVED);

		mockMvc.perform(post("/api/v1/candidates/{candidateId}/exclude", candidate.getId())
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(CandidateStatus.EXCLUDED.name()));
		assertThat(candidateRepository.findById(candidate.getId()).orElseThrow().getStatus())
				.isEqualTo(CandidateStatus.EXCLUDED);

		mockMvc.perform(get("/api/v1/candidates")
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0));
	}

	@Test
	void candidateDetailDoesNotExposeOtherUsersCandidate() throws Exception {
		AuthResponse ownerAuth = signup("candidate-private-owner@example.com");
		AuthResponse otherAuth = signup("candidate-private-other@example.com");
		User owner = userRepository.findById(ownerAuth.userId()).orElseThrow();
		ProductCandidate candidate = createCandidate(
				owner,
				"private candidate",
				CandidateSourceType.CSV,
				CategoryCode.TRAVEL_ORGANIZER,
				CandidateGrade.REVIEW,
				69,
				new BigDecimal("22.00"),
				RiskLevel.LOW
		);

		mockMvc.perform(get("/api/v1/candidates/{candidateId}", candidate.getId())
						.header("Authorization", bearer(otherAuth)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.CANDIDATE_NOT_FOUND.name()));
	}

	private ProductCandidate createCandidate(
			User user,
			String name,
			CandidateSourceType source,
			CategoryCode categoryCode,
			CandidateGrade grade,
			int score,
			BigDecimal marginRate,
			RiskLevel riskLevel
	) {
		ProductCandidate candidate = ProductCandidate.create(
				user,
				null,
				source,
				name,
				categoryCode,
				12_900,
				4_200,
				3_000,
				marginRate,
				grade
		);
		candidate.assignScore(CandidateScore.create(
				new ScoringBreakdown(20, 18, 22, 7, 5, riskLevel == RiskLevel.CAUTION ? -15 : 0),
				score,
				grade,
				riskLevel,
				List.of("steady trend"),
				List.of("review competition before sourcing")
		));
		return candidateRepository.saveAndFlush(candidate);
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
