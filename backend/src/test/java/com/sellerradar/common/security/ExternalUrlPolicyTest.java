package com.sellerradar.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalUrlPolicyTest {
	private final ExternalUrlPolicy policy = new ExternalUrlPolicy();

	@Test
	void acceptsHttpAndHttpsUrls() {
		assertThat(policy.normalize("https://example.com/item?q=1")).contains("https://example.com/item?q=1");
		assertThat(policy.normalize("http://example.com/image.jpg")).contains("http://example.com/image.jpg");
	}

	@Test
	void rejectsExecutableAndLocalSchemes() {
		assertThat(policy.normalize("javascript:alert(1)")).isEmpty();
		assertThat(policy.normalize("data:text/html,test")).isEmpty();
		assertThat(policy.normalize("file:///etc/passwd")).isEmpty();
	}

	@Test
	void rejectsProtocolRelativeUserInfoAndControlCharacters() {
		assertThat(policy.normalize("//example.com/item")).isEmpty();
		assertThat(policy.normalize("https://user:pass@example.com/item")).isEmpty();
		assertThat(policy.normalize("https://example.com/line\nbreak")).isEmpty();
	}

	@Test
	void rejectsRelativeAndMalformedUrls() {
		assertThat(policy.normalize("/relative/path")).isEmpty();
		assertThat(policy.normalize("not a url")).isEmpty();
	}
}
