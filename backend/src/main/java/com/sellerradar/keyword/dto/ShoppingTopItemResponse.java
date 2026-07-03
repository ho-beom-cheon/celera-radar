package com.sellerradar.keyword.dto;

public record ShoppingTopItemResponse(
		int itemRank,
		String title,
		String link,
		String image,
		Integer lprice,
		Integer hprice,
		String mallName,
		String category1,
		String category2,
		String category3,
		String category4
) {
}
