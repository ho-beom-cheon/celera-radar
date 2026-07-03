package com.sellerradar.wholesale.service;

import com.sellerradar.category.domain.CategoryCode;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CategoryCodeResolver {
	public CategoryCode resolve(String value) {
		if (value == null || value.isBlank()) {
			return CategoryCode.HOME_STORAGE;
		}
		String normalized = value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
		if (containsAny(normalized, "car", "auto", "vehicle", "차량", "자동차")) {
			return CategoryCode.CAR_ACCESSORY;
		}
		if (containsAny(normalized, "desk", "office", "cable", "사무", "책상")) {
			return CategoryCode.DESK_OFFICE;
		}
		if (containsAny(normalized, "bath", "clean", "욕실", "청소")) {
			return CategoryCode.BATH_CLEANING;
		}
		if (containsAny(normalized, "travel", "trip", "여행")) {
			return CategoryCode.TRAVEL_ORGANIZER;
		}
		if (containsAny(normalized, "pet", "dog", "cat", "반려", "강아지", "고양이")) {
			return CategoryCode.PET_WALK_HYGIENE;
		}
		if (containsAny(normalized, "kitchen", "cook", "주방")) {
			return CategoryCode.KITCHEN_STORAGE;
		}
		if (containsAny(normalized, "camp", "picnic", "캠핑", "피크닉")) {
			return CategoryCode.CAMPING_PICNIC;
		}
		if (containsAny(normalized, "training", "fitness", "운동", "헬스")) {
			return CategoryCode.HOME_TRAINING;
		}
		if (containsAny(normalized, "season", "summer", "winter", "시즌", "여름", "겨울")) {
			return CategoryCode.SEASONAL_LIVING;
		}
		return CategoryCode.HOME_STORAGE;
	}

	private boolean containsAny(String value, String... tokens) {
		for (String token : tokens) {
			if (value.contains(token)) {
				return true;
			}
		}
		return false;
	}
}
