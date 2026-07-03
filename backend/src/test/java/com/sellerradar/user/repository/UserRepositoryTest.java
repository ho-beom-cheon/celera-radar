package com.sellerradar.user.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sellerradar.plan.domain.Plan;
import com.sellerradar.plan.domain.SubscriptionPlan;
import com.sellerradar.plan.domain.UserSubscription;
import com.sellerradar.plan.domain.UserSubscriptionStatus;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.domain.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserRepositoryTest {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void saveUserCreatesFreeUserByDefault() {
		User savedUser = userRepository.saveAndFlush(User.create("seller@example.com", "{bcrypt}hash"));

		assertNotNull(savedUser.getId());
		assertNotNull(savedUser.getPublicId());
		assertEquals("seller@example.com", savedUser.getEmail());
		assertEquals("seller", savedUser.getDisplayName());
		assertEquals(UserRole.USER, savedUser.getRole());
		assertEquals(Plan.FREE, savedUser.getPlanCode());
		assertTrue(savedUser.isActive());
		assertNotNull(savedUser.getCreatedAt());
		assertNotNull(savedUser.getUpdatedAt());
		assertTrue(userRepository.existsByEmail("seller@example.com"));
		assertTrue(userRepository.findByEmail("seller@example.com").isPresent());
	}

	@Test
	void userSubscriptionCanReferenceFreePlanAndUser() {
		SubscriptionPlan freePlan = SubscriptionPlan.from(Plan.FREE);
		entityManager.persist(freePlan);

		User savedUser = userRepository.saveAndFlush(User.create("free-user@example.com", "{bcrypt}hash"));
		UserSubscription subscription = UserSubscription.free(savedUser, freePlan);
		entityManager.persist(subscription);
		entityManager.flush();
		entityManager.clear();

		UserSubscription foundSubscription = entityManager.find(UserSubscription.class, subscription.getId());

		assertNotNull(foundSubscription);
		assertEquals(UserSubscriptionStatus.ACTIVE, foundSubscription.getStatus());
		assertEquals(Plan.FREE, foundSubscription.getPlan().getCode());
		assertEquals(savedUser.getId(), foundSubscription.getUser().getId());
		assertNotNull(foundSubscription.getStartedAt());
		assertNotNull(foundSubscription.getCreatedAt());
		assertNotNull(foundSubscription.getUpdatedAt());
	}
}
