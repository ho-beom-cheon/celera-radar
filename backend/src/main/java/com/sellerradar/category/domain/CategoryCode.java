package com.sellerradar.category.domain;

public enum CategoryCode {
	CAR_ACCESSORY("차량용품"),
	DESK_OFFICE("데스크/오피스"),
	HOME_STORAGE("홈수납"),
	BATH_CLEANING("욕실/청소"),
	TRAVEL_ORGANIZER("여행정리"),
	PET_WALK_HYGIENE("반려동물 산책/위생"),
	KITCHEN_STORAGE("주방수납"),
	CAMPING_PICNIC("캠핑/피크닉"),
	HOME_TRAINING("홈트레이닝"),
	SEASONAL_LIVING("시즌생활");

	private final String displayName;

	CategoryCode(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}
}
