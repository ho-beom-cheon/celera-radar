package com.sellerradar.smartstore.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.smartstore.domain.SmartStoreProduct;
import com.sellerradar.smartstore.domain.StoreProductCost;
import com.sellerradar.smartstore.dto.StoreProductCostResponse;
import com.sellerradar.smartstore.dto.StoreProductCostUpsertRequest;
import com.sellerradar.smartstore.repository.SmartStoreProductRepository;
import com.sellerradar.smartstore.repository.StoreProductCostRepository;
import com.sellerradar.wholesale.domain.WholesaleProduct;
import com.sellerradar.wholesale.domain.WholesaleProductParseStatus;
import com.sellerradar.wholesale.repository.WholesaleProductRepository;
import com.sellerradar.wholesale.service.MarginCalculator;
import com.sellerradar.wholesale.service.MarginCalculator.MarginCalculationRequest;
import com.sellerradar.wholesale.service.MarginCalculator.MarginCalculationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreProductCostService {
	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private final SmartStoreProductRepository productRepository;
	private final StoreProductCostRepository costRepository;
	private final WholesaleProductRepository wholesaleProductRepository;
	private final MarginCalculator marginCalculator;

	public StoreProductCostService(
			SmartStoreProductRepository productRepository,
			StoreProductCostRepository costRepository,
			WholesaleProductRepository wholesaleProductRepository,
			MarginCalculator marginCalculator
	) {
		this.productRepository = productRepository;
		this.costRepository = costRepository;
		this.wholesaleProductRepository = wholesaleProductRepository;
		this.marginCalculator = marginCalculator;
	}

	@Transactional
	public StoreProductCostResponse upsert(Long userId, Long storeProductId, StoreProductCostUpsertRequest request) {
		SmartStoreProduct storeProduct = getStoreProduct(userId, storeProductId);
		WholesaleProduct wholesaleProduct = getWholesaleProduct(userId, request.wholesaleProductId());
		StoreProductCost cost = costRepository.findByStoreProductIdAndUserId(storeProductId, userId)
				.orElseGet(() -> StoreProductCost.create(storeProduct));
		cost.update(
				wholesaleProduct,
				resolvePurchaseCost(request, wholesaleProduct),
				resolveShippingFee(request, wholesaleProduct),
				defaultZero(request.packagingFee()),
				defaultZero(request.extraCost()),
				defaultZero(request.platformFeeRate()),
				request.targetMarginRate(),
				request.memo()
		);
		return toResponse(costRepository.save(cost));
	}

	@Transactional(readOnly = true)
	public StoreProductCostResponse get(Long userId, Long storeProductId) {
		getStoreProduct(userId, storeProductId);
		StoreProductCost cost = costRepository.findByStoreProductIdAndUserId(storeProductId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.STORE_PRODUCT_COST_NOT_FOUND));
		return toResponse(cost);
	}

	private SmartStoreProduct getStoreProduct(Long userId, Long storeProductId) {
		return productRepository.findByIdAndUserId(storeProductId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SMARTSTORE_PRODUCT_NOT_FOUND));
	}

	private WholesaleProduct getWholesaleProduct(Long userId, Long wholesaleProductId) {
		if (wholesaleProductId == null) {
			return null;
		}
		return wholesaleProductRepository.findByIdAndUserIdAndParseStatus(
						wholesaleProductId,
						userId,
						WholesaleProductParseStatus.PARSED
				)
				.orElseThrow(() -> new BusinessException(ErrorCode.WHOLESALE_PRODUCT_NOT_FOUND));
	}

	private BigDecimal resolvePurchaseCost(StoreProductCostUpsertRequest request, WholesaleProduct wholesaleProduct) {
		if (request.purchaseCost() != null) {
			return request.purchaseCost();
		}
		if (wholesaleProduct != null && wholesaleProduct.getSupplyPrice() != null) {
			return BigDecimal.valueOf(wholesaleProduct.getSupplyPrice());
		}
		throw new BusinessException(
				ErrorCode.INVALID_REQUEST,
				"purchaseCost is required when wholesaleProductId is not provided.",
				"purchaseCost"
		);
	}

	private BigDecimal resolveShippingFee(StoreProductCostUpsertRequest request, WholesaleProduct wholesaleProduct) {
		if (request.shippingFee() != null) {
			return request.shippingFee();
		}
		if (wholesaleProduct != null && wholesaleProduct.getShippingFee() != null) {
			return BigDecimal.valueOf(wholesaleProduct.getShippingFee());
		}
		return ZERO;
	}

	private BigDecimal defaultZero(BigDecimal value) {
		return value == null ? ZERO : value;
	}

	private StoreProductCostResponse toResponse(StoreProductCost cost) {
		SmartStoreProduct product = cost.getStoreProduct();
		WholesaleProduct wholesaleProduct = cost.getWholesaleProduct();
		MarginCalculationResult margin = marginCalculator.calculate(new MarginCalculationRequest(
				toWon(product.getSalePrice()),
				toWon(cost.getPurchaseCost()),
				toWon(cost.getShippingFee()),
				cost.getPlatformFeeRate(),
				0,
				0,
				toWon(cost.getPackagingFee().add(cost.getExtraCost())),
				cost.getTargetMarginRate()
		));
		return new StoreProductCostResponse(
				cost.getId(),
				product.getId(),
				wholesaleProduct == null ? null : wholesaleProduct.getId(),
				wholesaleProduct == null ? null : wholesaleProduct.getProductName(),
				cost.getPurchaseCost(),
				cost.getShippingFee(),
				cost.getPackagingFee(),
				cost.getExtraCost(),
				cost.getPlatformFeeRate(),
				cost.getTargetMarginRate(),
				product.getSalePrice(),
				margin.totalCost(),
				margin.expectedProfit(),
				margin.expectedMarginRate(),
				margin.recommendedSalePrice(),
				cost.getMemo(),
				cost.getCreatedAt(),
				cost.getUpdatedAt()
		);
	}

	private int toWon(BigDecimal value) {
		if (value == null || value.signum() < 0) {
			return 0;
		}
		return value.setScale(0, RoundingMode.HALF_UP).intValue();
	}
}
