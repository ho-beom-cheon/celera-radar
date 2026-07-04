package com.sellerradar.wholesale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WholesaleUploadConfirmRequest(
		@Valid @NotNull Mapping mapping
) {
	public record Mapping(
			@NotBlank String productName,
			@NotBlank String supplyPrice,
			String shippingFee,
			String imageUrl,
			String productUrl,
			String category
	) {
	}
}
