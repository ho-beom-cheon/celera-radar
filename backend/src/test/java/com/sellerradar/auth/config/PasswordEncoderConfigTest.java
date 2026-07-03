package com.sellerradar.auth.config;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class PasswordEncoderConfigTest {
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void passwordEncoderHashesAndMatchesRawPassword() {
		String rawPassword = "password1234";

		String encodedPassword = passwordEncoder.encode(rawPassword);

		assertNotEquals(rawPassword, encodedPassword);
		assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
	}
}
