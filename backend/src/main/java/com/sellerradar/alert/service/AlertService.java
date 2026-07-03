package com.sellerradar.alert.service;

import com.sellerradar.alert.domain.Alert;
import com.sellerradar.alert.domain.AlertRule;
import com.sellerradar.alert.dto.AlertResponse;
import com.sellerradar.alert.dto.AlertRuleCreateRequest;
import com.sellerradar.alert.dto.AlertRuleResponse;
import com.sellerradar.alert.repository.AlertRepository;
import com.sellerradar.alert.repository.AlertRuleRepository;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {
	private final AlertRepository alertRepository;
	private final AlertRuleRepository alertRuleRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@Autowired
	public AlertService(
			AlertRepository alertRepository,
			AlertRuleRepository alertRuleRepository,
			UserRepository userRepository
	) {
		this(alertRepository, alertRuleRepository, userRepository, Clock.systemDefaultZone());
	}

	AlertService(
			AlertRepository alertRepository,
			AlertRuleRepository alertRuleRepository,
			UserRepository userRepository,
			Clock clock
	) {
		this.alertRepository = alertRepository;
		this.alertRuleRepository = alertRuleRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional
	public AlertRuleResponse createRule(Long userId, AlertRuleCreateRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		AlertRule rule = AlertRule.create(
				user,
				request.name(),
				request.minScore(),
				request.minMarginRate(),
				request.categoryCodes(),
				request.riskExcluded(),
				request.frequency()
		);
		return AlertRuleResponse.from(alertRuleRepository.save(rule));
	}

	@Transactional(readOnly = true)
	public Page<AlertResponse> list(Long userId, Pageable pageable) {
		return alertRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
				.map(AlertResponse::from);
	}

	@Transactional
	public AlertResponse markRead(Long userId, Long alertId) {
		Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ALERT_NOT_FOUND));
		alert.markRead(OffsetDateTime.now(clock));
		return AlertResponse.from(alert);
	}
}
