package com.sellerradar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SellerRadarApplication {

	public static void main(String[] args) {
		SpringApplication.run(SellerRadarApplication.class, args);
	}

}
