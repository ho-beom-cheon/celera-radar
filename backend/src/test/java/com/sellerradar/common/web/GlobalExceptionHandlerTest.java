package com.sellerradar.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sellerradar.SellerRadarApplication;
import com.sellerradar.common.api.ApiResponse;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = {SellerRadarApplication.class, GlobalExceptionHandlerTest.TestController.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {
	private static final String REQUEST_ID = "req_test_001";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void requestIdHeaderIsReusedInSuccessResponse() throws Exception {
		mockMvc.perform(get("/test/common/ok")
						.requestAttr(RequestContext.REQUEST_ID_ATTRIBUTE, REQUEST_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.message").value("ok"))
				.andExpect(jsonPath("$.error").doesNotExist())
				.andExpect(jsonPath("$.meta.requestId").value(REQUEST_ID))
				.andExpect(jsonPath("$.meta.generatedAt").exists());
	}

	@Test
	void validationErrorUsesStandardEnvelope() throws Exception {
		mockMvc.perform(post("/test/common/validation")
						.requestAttr(RequestContext.REQUEST_ID_ATTRIBUTE, REQUEST_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.name()))
				.andExpect(jsonPath("$.error.field").value("name"))
				.andExpect(jsonPath("$.error.violations[0].field").value("name"))
				.andExpect(jsonPath("$.meta.requestId").exists());
	}

	@Test
	void businessExceptionUsesConfiguredStatusAndErrorCode() throws Exception {
		mockMvc.perform(get("/test/common/business")
						.requestAttr(RequestContext.REQUEST_ID_ATTRIBUTE, REQUEST_ID))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.DUPLICATED_KEYWORD.name()))
				.andExpect(jsonPath("$.error.field").value("keyword"))
				.andExpect(jsonPath("$.meta.requestId").value(REQUEST_ID));
	}

	@Test
	void unexpectedExceptionUsesInternalServerErrorEnvelope() throws Exception {
		mockMvc.perform(get("/test/common/unexpected")
						.requestAttr(RequestContext.REQUEST_ID_ATTRIBUTE, REQUEST_ID))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.INTERNAL_SERVER_ERROR.name()))
				.andExpect(jsonPath("$.meta.requestId").value(REQUEST_ID));
	}

	@RestController
	@RequestMapping("/test/common")
	static class TestController {
		@GetMapping("/ok")
		ApiResponse<TestResponse> ok(HttpServletRequest request) {
			return ApiResponse.success(new TestResponse("ok"), RequestContext.requestId(request));
		}

		@PostMapping("/validation")
		ApiResponse<TestResponse> validation(@Valid @RequestBody TestRequest request, HttpServletRequest servletRequest) {
			return ApiResponse.success(new TestResponse(request.name()), RequestContext.requestId(servletRequest));
		}

		@GetMapping("/business")
		void business() {
			throw new BusinessException(ErrorCode.DUPLICATED_KEYWORD, "이미 등록된 키워드입니다.", "keyword");
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("unexpected");
		}
	}

	record TestRequest(
			@NotBlank(message = "이름은 필수입니다.")
			String name
	) {
	}

	record TestResponse(String message) {
	}
}
