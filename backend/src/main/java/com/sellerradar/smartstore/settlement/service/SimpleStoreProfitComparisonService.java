package com.sellerradar.smartstore.settlement.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class SimpleStoreProfitComparisonService implements StoreProfitComparisonService {
	private static final BigDecimal ZERO = BigDecimal.ZERO;

	@Override
	public StoreProfitComparison compare(StoreProfitComparisonInput input) {
		BigDecimal estimatedProfit = safe(input.estimatedProfit());
		BigDecimal actualSettlementAmount = safe(input.actualSettlementAmount());
		BigDecimal actualProfit = actualSettlementAmount
				.subtract(safe(input.purchaseCost()))
				.subtract(safe(input.actualFeeAmount()));
		BigDecimal profitGap = actualProfit.subtract(estimatedProfit);
		return new StoreProfitComparison(
				estimatedProfit,
				actualProfit,
				profitGap,
				status(estimatedProfit, actualProfit)
		);
	}

	private ProfitComparisonStatus status(BigDecimal estimatedProfit, BigDecimal actualProfit) {
		if (estimatedProfit.signum() == 0 && actualProfit.signum() == 0) {
			return ProfitComparisonStatus.UNKNOWN;
		}
		int comparison = actualProfit.compareTo(estimatedProfit);
		if (comparison == 0) {
			return ProfitComparisonStatus.MATCHED;
		}
		return comparison < 0
				? ProfitComparisonStatus.LOWER_THAN_EXPECTED
				: ProfitComparisonStatus.HIGHER_THAN_EXPECTED;
	}

	private BigDecimal safe(BigDecimal value) {
		return value == null ? ZERO : value;
	}
}
