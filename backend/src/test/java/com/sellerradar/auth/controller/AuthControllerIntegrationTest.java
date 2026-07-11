package com.sellerradar.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.SellerRadarApplication;
import com.sellerradar.auth.dto.AuthResponse;
import com.sellerradar.auth.dto.LoginRequest;
import com.sellerradar.auth.dto.RefreshTokenRequest;
import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.auth.session.AuthSessionRepository;
import com.sellerradar.auth.session.RefreshTokenCodec;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.web.RequestContext;
import com.sellerradar.plan.domain.Plan;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(classes = {SellerRadarApplication.class, AuthControllerIntegrationTest.ProtectedController.class})
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
	private static final String REQUEST_ID = "req_auth_test_001";
	private static final String EMAIL = "seller@example.com";
	private static final String PASSWORD = "password1234";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuthSessionRepository authSessionRepository;

	@Autowired
	private RefreshTokenCodec refreshTokenCodec;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		authSessionRepository.deleteAllInBatch();
		userRepository.deleteAll();
	}

	@Test
	void signupCreatesFreeUserAndReturnsJwtTokens() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.header(RequestContext.REQUEST_ID_HEADER, REQUEST_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SignupRequest(EMAIL, PASSWORD, true))))
				.andExpect(status().isOk())
				.andExpect(header().string(RequestContext.REQUEST_ID_HEADER, REQUEST_ID))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.email").value(EMAIL))
				.andExpect(jsonPath("$.data.plan").value(Plan.FREE.name()))
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.meta.requestId").value(REQUEST_ID))
				.andReturn();

		JsonNode data = dataNode(result);
		User savedUser = userRepository.findByEmail(EMAIL).orElseThrow();

		assertThat(data.get("userId").asLong()).isEqualTo(savedUser.getId());
		assertThat(savedUser.getPlanCode()).isEqualTo(Plan.FREE);
		assertThat(savedUser.getPasswordHash()).isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, savedUser.getPasswordHash())).isTrue();
		assertThat(data.get("refreshToken").asText()).doesNotContain(".");
		assertThat(authSessionRepository.findAll()).singleElement().satisfies(session -> {
			assertThat(session.getTokenHash()).isEqualTo(refreshTokenCodec.hash(data.get("refreshToken").asText()));
			assertThat(session.getTokenHash()).doesNotContain(data.get("refreshToken").asText());
		});
	}

	@Test
	void signupRejectsDuplicatedEmail() throws Exception {
		userRepository.save(User.create(EMAIL, passwordEncoder.encode(PASSWORD)));

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SignupRequest(EMAIL, PASSWORD, true))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.DUPLICATED_EMAIL.name()))
				.andExpect(jsonPath("$.error.field").value("email"));
	}

	@Test
	void signupRejectsShortPassword() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SignupRequest(EMAIL, "short", true))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.name()))
				.andExpect(jsonPath("$.error.field").value("password"));
	}

	@Test
	void loginReturnsJwtTokensForValidCredentials() throws Exception {
		userRepository.save(User.create(EMAIL, passwordEncoder.encode(PASSWORD)));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.email").value(EMAIL))
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
	}

	@Test
	void loginRejectsInvalidCredentials() throws Exception {
		userRepository.save(User.create(EMAIL, passwordEncoder.encode(PASSWORD)));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "wrong-pass"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_CREDENTIALS.name()));
	}

	@Test
	void refreshIssuesNewTokenPairForValidRefreshToken() throws Exception {
		AuthResponse authResponse = signupAndReadAuthResponse();

		MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RefreshTokenRequest(authResponse.refreshToken()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.userId").value(authResponse.userId()))
				.andExpect(jsonPath("$.data.email").value(EMAIL))
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
				.andReturn();

		AuthResponse rotated = authResponse(dataNode(result));
		assertThat(rotated.refreshToken()).isNotEqualTo(authResponse.refreshToken());
		assertThat(authSessionRepository.findAll()).hasSize(2);
		assertThat(authSessionRepository.findByTokenHash(refreshTokenCodec.hash(authResponse.refreshToken())))
				.get()
				.extracting(session -> session.getRotatedAt())
				.isNotNull();
	}

	@Test
	void reusedRefreshTokenRevokesTheWholeFamily() throws Exception {
		AuthResponse initial = signupAndReadAuthResponse();
		AuthResponse rotated = refreshAndReadAuthResponse(initial.refreshToken());

		refreshExpectUnauthorized(initial.refreshToken());
		refreshExpectUnauthorized(rotated.refreshToken());

		assertThat(authSessionRepository.findAll())
				.allSatisfy(session -> assertThat(session.getRevokedAt()).isNotNull());
		assertThat(authSessionRepository.findByTokenHash(refreshTokenCodec.hash(initial.refreshToken())))
				.get()
				.extracting(session -> session.getReuseDetectedAt())
				.isNotNull();
	}

	@Test
	void logoutRevokesTheCurrentRefreshFamily() throws Exception {
		AuthResponse auth = signupAndReadAuthResponse();

		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RefreshTokenRequest(auth.refreshToken()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		refreshExpectUnauthorized(auth.refreshToken());
	}

	@Test
	void logoutAllRevokesEveryRefreshSessionForAuthenticatedUser() throws Exception {
		AuthResponse first = signupAndReadAuthResponse();
		AuthResponse second = loginAndReadAuthResponse();

		mockMvc.perform(post("/api/v1/auth/logout-all")
						.header("Authorization", "Bearer " + first.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		refreshExpectUnauthorized(first.refreshToken());
		refreshExpectUnauthorized(second.refreshToken());
	}

	@Test
	void inactiveUserCannotLoginRefreshOrUseExistingAccessToken() throws Exception {
		AuthResponse auth = signupAndReadAuthResponse();
		User user = userRepository.findById(auth.userId()).orElseThrow();
		user.delete();
		userRepository.saveAndFlush(user);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_CREDENTIALS.name()));

		refreshExpectUnauthorized(auth.refreshToken());

		mockMvc.perform(get("/test/security/protected")
						.header("Authorization", "Bearer " + auth.accessToken()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.AUTH_REQUIRED.name()));
	}

	@Test
	void refreshRejectsInvalidRefreshToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RefreshTokenRequest("invalid.refresh.token"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REFRESH_TOKEN.name()));
	}

	@Test
	void protectedEndpointRequiresBearerToken() throws Exception {
		mockMvc.perform(get("/test/security/protected"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.AUTH_REQUIRED.name()));
	}

	@Test
	void protectedEndpointAcceptsAccessToken() throws Exception {
		AuthResponse authResponse = signupAndReadAuthResponse();

		mockMvc.perform(get("/test/security/protected")
						.header("Authorization", "Bearer " + authResponse.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("ok"));
	}

	private AuthResponse signupAndReadAuthResponse() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SignupRequest(EMAIL, PASSWORD, true))))
				.andExpect(status().isOk())
				.andReturn();
		return authResponse(dataNode(result));
	}

	private AuthResponse loginAndReadAuthResponse() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
				.andExpect(status().isOk())
				.andReturn();
		return authResponse(dataNode(result));
	}

	private AuthResponse refreshAndReadAuthResponse(String refreshToken) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
				.andExpect(status().isOk())
				.andReturn();
		return authResponse(dataNode(result));
	}

	private void refreshExpectUnauthorized(String refreshToken) throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REFRESH_TOKEN.name()));
	}

	private AuthResponse authResponse(JsonNode data) {
		return new AuthResponse(
				data.get("userId").asLong(),
				data.get("email").asText(),
				Plan.valueOf(data.get("plan").asText()),
				data.get("accessToken").asText(),
				data.get("refreshToken").asText()
		);
	}

	private JsonNode dataNode(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
	}

	@RestController
	@RequestMapping("/test/security")
	static class ProtectedController {
		@GetMapping("/protected")
		TestResponse protectedEndpoint() {
			return new TestResponse("ok");
		}
	}

	record TestResponse(String message) {
	}
}
