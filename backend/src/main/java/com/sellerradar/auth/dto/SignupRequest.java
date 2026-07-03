package com.sellerradar.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		String email,

		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
		String password,

		@AssertTrue(message = "이용약관 동의가 필요합니다.")
		Boolean termsAgreed
) {
}
