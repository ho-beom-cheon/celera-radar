package com.sellerradar.batch.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.auth.jwt.JwtTokenProvider;
import com.sellerradar.batch.repository.BatchJobHistoryRepository;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.domain.UserRole;
import com.sellerradar.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminBatchControllerIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private BatchJobHistoryRepository historyRepository;

	@Autowired
	private KeywordRepository keywordRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		historyRepository.deleteAll();
		keywordRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void adminBatchEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/admin/batches"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
	}

	@Test
	void adminBatchEndpointsRejectUserRole() throws Exception {
		mockMvc.perform(post("/api/v1/admin/batches/shopping-search/run")
						.header("Authorization", bearerToken(UserRole.USER)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	@Test
	void adminCanRunShoppingSearchBatch() throws Exception {
		mockMvc.perform(post("/api/v1/admin/batches/shopping-search/run")
						.header("Authorization", bearerToken(UserRole.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.jobType").value("SHOPPING_SEARCH_DAILY"))
				.andExpect(jsonPath("$.data.triggerType").value("MANUAL"))
				.andExpect(jsonPath("$.data.status").value("SUCCESS"))
				.andExpect(jsonPath("$.data.targetCount").value(0))
				.andExpect(jsonPath("$.data.failureCount").value(0));
	}

	@Test
	void adminCanRunDatalabTrendBatch() throws Exception {
		mockMvc.perform(post("/api/v1/admin/batches/datalab/run")
						.header("Authorization", bearerToken(UserRole.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.jobType").value("DATALAB_TREND_DAILY"))
				.andExpect(jsonPath("$.data.triggerType").value("MANUAL"))
				.andExpect(jsonPath("$.data.status").value("SUCCESS"))
				.andExpect(jsonPath("$.data.targetCount").value(0))
				.andExpect(jsonPath("$.data.failureCount").value(0));
	}

	@Test
	void adminCanListBatchHistories() throws Exception {
		mockMvc.perform(post("/api/v1/admin/batches/shopping-search/run")
						.header("Authorization", bearerToken(UserRole.ADMIN)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/admin/batches")
						.header("Authorization", bearerToken(UserRole.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].jobType").value("SHOPPING_SEARCH_DAILY"))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	private String bearerToken(UserRole role) {
		String email = role.name().toLowerCase() + "@example.com";
		User user = userRepository.findByEmail(email).orElseGet(() -> {
			User created = User.create(email, "{bcrypt}hash");
			ReflectionTestUtils.setField(created, "role", role);
			return userRepository.save(created);
		});
		return "Bearer " + jwtTokenProvider.issueAccessToken(user);
	}
}
