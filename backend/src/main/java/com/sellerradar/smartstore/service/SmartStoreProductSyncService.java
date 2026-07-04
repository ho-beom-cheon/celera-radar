package com.sellerradar.smartstore.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.smartstore.client.SmartStoreClient;
import com.sellerradar.smartstore.client.SmartStoreProductSyncItem;
import com.sellerradar.smartstore.domain.SmartStoreConnection;
import com.sellerradar.smartstore.domain.SmartStoreProduct;
import com.sellerradar.smartstore.domain.SmartStoreProductSyncHistory;
import com.sellerradar.smartstore.dto.SmartStoreProductResponse;
import com.sellerradar.smartstore.dto.SmartStoreProductSyncResponse;
import com.sellerradar.smartstore.repository.SmartStoreConnectionRepository;
import com.sellerradar.smartstore.repository.SmartStoreProductRepository;
import com.sellerradar.smartstore.repository.SmartStoreProductSyncHistoryRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmartStoreProductSyncService {
	private final SmartStoreConnectionRepository connectionRepository;
	private final SmartStoreProductRepository productRepository;
	private final SmartStoreProductSyncHistoryRepository historyRepository;
	private final SmartStoreClient smartStoreClient;
	private final Clock clock;

	@Autowired
	public SmartStoreProductSyncService(
			SmartStoreConnectionRepository connectionRepository,
			SmartStoreProductRepository productRepository,
			SmartStoreProductSyncHistoryRepository historyRepository,
			SmartStoreClient smartStoreClient
	) {
		this(connectionRepository, productRepository, historyRepository, smartStoreClient, Clock.systemDefaultZone());
	}

	SmartStoreProductSyncService(
			SmartStoreConnectionRepository connectionRepository,
			SmartStoreProductRepository productRepository,
			SmartStoreProductSyncHistoryRepository historyRepository,
			SmartStoreClient smartStoreClient,
			Clock clock
	) {
		this.connectionRepository = connectionRepository;
		this.productRepository = productRepository;
		this.historyRepository = historyRepository;
		this.smartStoreClient = smartStoreClient;
		this.clock = clock;
	}

	@Transactional
	public SmartStoreProductSyncResponse sync(Long userId, Long connectionId) {
		SmartStoreConnection connection = getConnection(userId, connectionId);
		OffsetDateTime startedAt = OffsetDateTime.now(clock);
		SmartStoreProductSyncHistory history = historyRepository.save(SmartStoreProductSyncHistory.start(
				connection,
				startedAt
		));
		try {
			List<SmartStoreProductSyncItem> items = smartStoreClient.fetchProducts(connection);
			OffsetDateTime syncedAt = OffsetDateTime.now(clock);
			int successCount = 0;
			for (SmartStoreProductSyncItem item : items) {
				upsertProduct(connection, item, syncedAt);
				successCount++;
			}
			connection.markSynced(syncedAt);
			history.complete(items.size(), successCount, 0, syncedAt);
			return new SmartStoreProductSyncResponse(
					history.getId(),
					connection.getId(),
					history.getStatus(),
					history.getTargetCount(),
					history.getSuccessCount(),
					history.getFailureCount(),
					syncedAt
			);
		} catch (RuntimeException exception) {
			OffsetDateTime failedAt = OffsetDateTime.now(clock);
			history.fail(exception.getMessage(), failedAt);
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public Page<SmartStoreProductResponse> list(Long userId, Pageable pageable) {
		return productRepository.findByUserIdOrderByLastSyncedAtDesc(userId, pageable)
				.map(SmartStoreProductResponse::from);
	}

	private SmartStoreConnection getConnection(Long userId, Long connectionId) {
		return connectionRepository.findByIdAndUserId(connectionId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SMARTSTORE_CONNECTION_NOT_FOUND));
	}

	private void upsertProduct(
			SmartStoreConnection connection,
			SmartStoreProductSyncItem item,
			OffsetDateTime syncedAt
	) {
		SmartStoreProduct product = productRepository.findByConnectionIdAndSourceProductId(
						connection.getId(),
						item.sourceProductId()
				)
				.orElseGet(() -> SmartStoreProduct.create(connection, item, syncedAt));
		product.sync(item, syncedAt);
		productRepository.save(product);
	}
}
