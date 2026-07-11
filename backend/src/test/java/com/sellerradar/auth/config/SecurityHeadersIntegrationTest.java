package com.sellerradar.auth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityHeadersIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiResponseIncludesReportOnlyAndBrowserSecurityHeaders() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().exists("Content-Security-Policy-Report-Only"))
				.andExpect(header().doesNotExist("Content-Security-Policy"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "no-referrer"))
				.andExpect(header().string("Cross-Origin-Opener-Policy", "same-origin"));
	}

	@Test
	void corsAllowsOnlyConfiguredDevelopmentOrigin() throws Exception {
		mockMvc.perform(options("/api/v1/auth/login")
						.header("Origin", "http://localhost:5173")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

		mockMvc.perform(options("/api/v1/auth/login")
						.header("Origin", "https://evil.example")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}
