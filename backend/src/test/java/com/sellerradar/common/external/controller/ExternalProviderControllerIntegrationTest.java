package com.sellerradar.common.external.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.auth.dto.SignupRequest;
import com.sellerradar.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "seller-radar.external.naver.mode=DISABLED")
@AutoConfigureMockMvc
class ExternalProviderControllerIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
	}

	@Test
	void authenticatedUserCanReadDisabledCapabilitiesWithoutExternalCall() throws Exception {
		String token = signupAndGetAccessToken();

		mockMvc.perform(get("/api/v1/external/providers/naver/capabilities")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.provider").value("NAVER"))
				.andExpect(jsonPath("$.data.mode").value("DISABLED"))
				.andExpect(jsonPath("$.data.capabilities").isEmpty());
	}

	private String signupAndGetAccessToken() throws Exception {
		var result = mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new SignupRequest(
								"provider-capabilities@example.com",
								"password1234",
								true
						))))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsByteArray())
				.get("data")
				.get("accessToken")
				.asText();
	}
}
