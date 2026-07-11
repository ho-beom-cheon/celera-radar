package com.sellerradar.trend.service;

import com.sellerradar.category.domain.CategoryCode;
import org.springframework.stereotype.Component;

@Component
public class NaverShoppingCategoryCodeResolver {
	private static final String FASHION_ACCESSORIES = "50000001";
	private static final String FURNITURE_INTERIOR = "50000004";
	private static final String SPORTS_LEISURE = "50000007";
	private static final String LIVING_HEALTH = "50000008";

	public String resolve(String category) {
		if (category != null && category.matches("\\d{8}")) {
			return category;
		}
		if (category == null || category.isBlank()) {
			return LIVING_HEALTH;
		}
		try {
			return resolve(CategoryCode.valueOf(category));
		} catch (IllegalArgumentException exception) {
			return LIVING_HEALTH;
		}
	}

	public String resolve(CategoryCode categoryCode) {
		if (categoryCode == null) {
			return LIVING_HEALTH;
		}
		return switch (categoryCode) {
			case TRAVEL_ORGANIZER -> FASHION_ACCESSORIES;
			case HOME_STORAGE -> FURNITURE_INTERIOR;
			case CAMPING_PICNIC, HOME_TRAINING -> SPORTS_LEISURE;
			case CAR_ACCESSORY, DESK_OFFICE, BATH_CLEANING, PET_WALK_HYGIENE,
					KITCHEN_STORAGE, SEASONAL_LIVING -> LIVING_HEALTH;
		};
	}
}
