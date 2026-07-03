package com.sellerradar.alert.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.alert.domain.AlertFrequency;
import com.sellerradar.alert.domain.AlertStatus;
import com.sellerradar.alert.dto.AlertRuleCreateRequest;
import com.sellerradar.alert.repository.AlertRepository;
import com.sellerradar.alert.repository.AlertRuleRepository;
import com.sellerradar.alert.service.AlertGenerationResult;
import com.sellerradar.alert.service.AlertGenerationService;
import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.candidate.domain.CandidateScore;
import com.sellerradar.candidate.domain.CandidateSourceType;
import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.domain.RiskLevel;
import com.sellerradar.candidate.repository.ProductCandidateRepository;
import com.sellerradar.category.domain.CategoryCode;
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
class AlertControllerIntegrationTest {
	private static final String PASSWORD = "password1234";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AlertRepository alertRepository;

	@Autowired
	private AlertRuleRepository alertRuleRepository;

	@Autowired
	private AlertGenerationService alertGenerationService;

	@Autowired
	private ProductCandidateRepository candidateRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		alertRepository.deleteAll();
		alertRuleRepository.deleteAll();
	}

	@Test
	void createRuleAndGenerateMatchingAlert() throws Exception {
		AuthResponse auth = signup("alert-match@example.com");
		User user = userRepository.findById(auth.userId()).orElseThrow();
		createCandidate(user, "car brush", CategoryCode.CAR_ACCESSORY, 86, new BigDecimal("31.20"), RiskLevel.LOW);
		createCandidate(user, "desk tray", CategoryCode.DESK_OFFICE, 70, new BigDecimal("18.00"), RiskLevel.LOW);

		createRule(auth, new AlertRuleCreateRequest(
				"score over 80",
				80,
				new BigDecimal("25"),
				List.of(CategoryCode.CAR_ACCESSORY),
				true,
				AlertFrequency.DAILY_SUMMARY
		));

		AlertGenerationResult result = alertGenerationService.generateDaily();

		assertThat(result.targetRuleCount()).isEqualTo(1);
		assertThat(result.generatedCount()).isEqualTo(1);
		mockMvc.perform(get("/api/v1/alerts")
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].candidateName").value("car brush"))
				.andExpect(jsonPath("$.data.items[0].status").value(AlertStatus.UNREAD.name()));
	}

	@Test
	void alertGenerationDoesNotCreateDuplicatesForSameRuleAndCandidate() throws Exception {
		AuthResponse auth = signup("alert-duplicate@example.com");
		User user = userRepository.findById(auth.userId()).orElseThrow();
		createCandidate(user, "storage box", CategoryCode.HOME_STORAGE, 88, new BigDecimal("35.00"), RiskLevel.LOW);
		createRule(auth, new AlertRuleCreateRequest(
				"score over 80",
				80,
				new BigDecimal("20"),
				List.of(),
				true,
				AlertFrequency.DAILY_SUMMARY
		));

		AlertGenerationResult first = alertGenerationService.generateDaily();
		AlertGenerationResult second = alertGenerationService.generateDaily();

		assertThat(first.generatedCount()).isEqualTo(1);
		assertThat(second.generatedCount()).isZero();
		assertThat(alertRepository.findAll()).hasSize(1);
	}

	@Test
	void markAlertReadUpdatesStatus() throws Exception {
		AuthResponse auth = signup("alert-read@example.com");
		User user = userRepository.findById(auth.userId()).orElseThrow();
		createCandidate(user, "camping pouch", CategoryCode.CAMPING_PICNIC, 83, new BigDecimal("28.00"), RiskLevel.LOW);
		createRule(auth, new AlertRuleCreateRequest(
				"score over 80",
				80,
				new BigDecimal("20"),
				List.of(CategoryCode.CAMPING_PICNIC),
				true,
				AlertFrequency.DAILY_SUMMARY
		));
		alertGenerationService.generateDaily();
		Long alertId = alertRepository.findAll().getFirst().getId();

		mockMvc.perform(patch("/api/v1/alerts/{alertId}/read", alertId)
						.header("Authorization", bearer(auth)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value(AlertStatus.READ.name()))
				.andExpect(jsonPath("$.data.readAt").isNotEmpty());
	}

	private void createRule(AuthResponse auth, AlertRuleCreateRequest request) throws Exception {
		mockMvc.perform(post("/api/v1/alert-rules")
						.header("Authorization", bearer(auth))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").isNumber());
	}

	private ProductCandidate createCandidate(
			User user,
			String name,
			CategoryCode categoryCode,
			int score,
			BigDecimal marginRate,
			RiskLevel riskLevel
	) {
		ProductCandidate candidate = ProductCandidate.create(
				user,
				null,
				CandidateSourceType.CSV,
				name,
				categoryCode,
				12_900,
				4_200,
				3_000,
				marginRate,
				CandidateGrade.fromScore(score)
		);
		candidate.assignScore(CandidateScore.create(
				new ScoringBreakdown(20, 20, 25, 10, 5, riskLevel == RiskLevel.EXCLUDED ? -40 : 0),
				score,
				CandidateGrade.fromScore(score),
				riskLevel,
				List.of("condition matched"),
				List.of("review before sourcing")
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
