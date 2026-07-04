package com.sellerradar.smartstore.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.smartstore.commission.domain.NaverCommissionSnapshot;
import com.sellerradar.smartstore.commission.repository.NaverCommissionSnapshotRepository;
import com.sellerradar.smartstore.domain.SmartStoreConnection;
import com.sellerradar.smartstore.order.domain.NaverOrderSnapshot;
import com.sellerradar.smartstore.order.repository.NaverOrderSnapshotRepository;
import com.sellerradar.smartstore.repository.SmartStoreConnectionRepository;
import com.sellerradar.smartstore.settlement.client.MockSmartStoreSettlementClient;
import com.sellerradar.smartstore.settlement.client.SmartStoreCommissionSnapshotItem;
import com.sellerradar.smartstore.settlement.client.SmartStoreOrderSnapshotItem;
import com.sellerradar.smartstore.settlement.client.SmartStoreSettlementSnapshotItem;
import com.sellerradar.smartstore.settlement.domain.NaverSettlementSnapshot;
import com.sellerradar.smartstore.settlement.repository.NaverSettlementSnapshotRepository;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SmartStoreSettlementSkeletonTest {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SmartStoreConnectionRepository connectionRepository;

	@Autowired
	private NaverOrderSnapshotRepository orderRepository;

	@Autowired
	private NaverSettlementSnapshotRepository settlementRepository;

	@Autowired
	private NaverCommissionSnapshotRepository commissionRepository;

	@Autowired
	private StoreProfitComparisonService profitComparisonService;

	@Test
	void snapshotRepositoriesStoreOrderSettlementAndCommissionData() {
		SmartStoreConnection connection = createConnection("settlement-snapshot@example.com");
		String productOrderNo = "product-order-001";
		LocalDate baseDate = LocalDate.of(2026, 7, 4);

		orderRepository.saveAndFlush(NaverOrderSnapshot.create(
				connection,
				"order-001",
				productOrderNo,
				OffsetDateTime.parse("2026-07-04T10:00:00+09:00"),
				new BigDecimal("12900"),
				1,
				"PAYED",
				"{\"order\":true}"
		));
		settlementRepository.saveAndFlush(NaverSettlementSnapshot.create(
				connection,
				baseDate,
				productOrderNo,
				new BigDecimal("12100"),
				"{\"settlement\":true}"
		));
		commissionRepository.saveAndFlush(NaverCommissionSnapshot.create(
				connection,
				baseDate,
				productOrderNo,
				"SALE_COMMISSION",
				new BigDecimal("516"),
				"{\"commission\":true}"
		));

		assertThat(orderRepository.findByConnectionIdAndProductOrderNo(connection.getId(), productOrderNo))
				.isPresent()
				.get()
				.extracting(NaverOrderSnapshot::getOrderStatus)
				.isEqualTo("PAYED");
		assertThat(settlementRepository.findByConnectionIdAndSettlementDateAndProductOrderNo(
				connection.getId(),
				baseDate,
				productOrderNo
		)).isPresent();
		assertThat(commissionRepository.findByConnectionIdAndBaseDateAndProductOrderNoAndCommissionType(
				connection.getId(),
				baseDate,
				productOrderNo,
				"SALE_COMMISSION"
		)).isPresent();
	}

	@Test
	void mockSettlementClientReturnsSeedDataWithoutExternalCall() {
		SmartStoreConnection connection = createConnection("settlement-mock@example.com");
		SmartStoreOrderSnapshotItem order = new SmartStoreOrderSnapshotItem(
				"order-001",
				"product-order-001",
				OffsetDateTime.parse("2026-07-04T10:00:00+09:00"),
				new BigDecimal("12900"),
				1,
				"PAYED",
				"{}"
		);
		SmartStoreSettlementSnapshotItem settlement = new SmartStoreSettlementSnapshotItem(
				LocalDate.of(2026, 7, 4),
				"product-order-001",
				new BigDecimal("12100"),
				"{}"
		);
		SmartStoreCommissionSnapshotItem commission = new SmartStoreCommissionSnapshotItem(
				LocalDate.of(2026, 7, 4),
				"product-order-001",
				"SALE_COMMISSION",
				new BigDecimal("516"),
				"{}"
		);
		MockSmartStoreSettlementClient client = new MockSmartStoreSettlementClient(
				List.of(order),
				List.of(settlement),
				List.of(commission)
		);

		assertThat(client.fetchOrders(connection, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4)))
				.containsExactly(order);
		assertThat(client.fetchSettlements(connection, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4)))
				.containsExactly(settlement);
		assertThat(client.fetchCommissions(connection, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4)))
				.containsExactly(commission);
	}

	@Test
	void profitComparisonServiceComparesEstimatedAndActualProfit() {
		StoreProfitComparison comparison = profitComparisonService.compare(new StoreProfitComparisonInput(
				new BigDecimal("2500"),
				new BigDecimal("12100"),
				new BigDecimal("9000"),
				new BigDecimal("800")
		));

		assertThat(comparison.actualProfit()).isEqualByComparingTo(new BigDecimal("2300"));
		assertThat(comparison.profitGap()).isEqualByComparingTo(new BigDecimal("-200"));
		assertThat(comparison.status()).isEqualTo(ProfitComparisonStatus.LOWER_THAN_EXPECTED);
	}

	private SmartStoreConnection createConnection(String email) {
		User user = userRepository.save(User.create(email, "{bcrypt}hash"));
		return connectionRepository.saveAndFlush(SmartStoreConnection.disconnected(
				user,
				"settlement store",
				"settlement-store-" + user.getId(),
				"settlement-seller-" + user.getId()
		));
	}
}
