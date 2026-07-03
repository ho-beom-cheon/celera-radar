package com.sellerradar.smartstore.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.smartstore.domain.SmartStoreConnection;
import com.sellerradar.smartstore.dto.SmartStoreConnectionStatusResponse;
import com.sellerradar.smartstore.repository.SmartStoreConnectionRepository;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmartStoreConnectionService {
	private final SmartStoreConnectionRepository connectionRepository;
	private final UserRepository userRepository;

	public SmartStoreConnectionService(
			SmartStoreConnectionRepository connectionRepository,
			UserRepository userRepository
	) {
		this.connectionRepository = connectionRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public SmartStoreConnectionStatusResponse getLatestStatus(Long userId) {
		return connectionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
				.map(SmartStoreConnectionStatusResponse::from)
				.orElseGet(SmartStoreConnectionStatusResponse::disconnected);
	}

	@Transactional
	public SmartStoreConnectionStatusResponse registerDisconnected(
			Long userId,
			String storeName,
			String storeId,
			String sellerId
	) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		SmartStoreConnection connection = SmartStoreConnection.disconnected(user, storeName, storeId, sellerId);
		return SmartStoreConnectionStatusResponse.from(connectionRepository.save(connection));
	}
}
