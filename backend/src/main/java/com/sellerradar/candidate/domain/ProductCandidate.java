package com.sellerradar.candidate.domain;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.scoring.CandidateGrade;
import com.sellerradar.scoring.ScoringBreakdown;
import com.sellerradar.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "product_candidate")
public class ProductCandidate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "keyword_id")
	private Keyword keyword;

	@Column(name = "wholesale_product_id")
	private Long wholesaleProductId;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 20)
	private CandidateSourceType sourceType;

	@Column(nullable = false, length = 255)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "category_code", nullable = false, length = 50)
	private CategoryCode categoryCode;

	@Column(name = "expected_sale_price", nullable = false)
	private int expectedSalePrice;

	@Column(name = "supply_price")
	private Integer supplyPrice;

	@Column(name = "shipping_fee")
	private Integer shippingFee;

	@Column(name = "expected_margin_rate", nullable = false, precision = 6, scale = 2)
	private BigDecimal expectedMarginRate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CandidateGrade grade;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CandidateStatus status;

	@OneToOne(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private CandidateScore score;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected ProductCandidate() {
	}

	private ProductCandidate(
			User user,
			Keyword keyword,
			CandidateSourceType sourceType,
			String name,
			CategoryCode categoryCode,
			int expectedSalePrice,
			Integer supplyPrice,
			Integer shippingFee,
			BigDecimal expectedMarginRate,
			CandidateGrade grade
	) {
		this.user = user;
		this.keyword = keyword;
		this.sourceType = sourceType;
		this.name = name;
		this.categoryCode = categoryCode;
		this.expectedSalePrice = expectedSalePrice;
		this.supplyPrice = supplyPrice;
		this.shippingFee = shippingFee;
		this.expectedMarginRate = expectedMarginRate;
		this.grade = grade;
		this.status = CandidateStatus.ACTIVE;
	}

	public static ProductCandidate create(
			User user,
			Keyword keyword,
			CandidateSourceType sourceType,
			String name,
			CategoryCode categoryCode,
			int expectedSalePrice,
			Integer supplyPrice,
			Integer shippingFee,
			BigDecimal expectedMarginRate,
			CandidateGrade grade
	) {
		return new ProductCandidate(
				user,
				keyword,
				sourceType,
				name,
				categoryCode,
				expectedSalePrice,
				supplyPrice,
				shippingFee,
				expectedMarginRate,
				grade
		);
	}

	public void assignScore(CandidateScore score) {
		score.attachTo(this);
		this.score = score;
		this.grade = score.getGrade();
	}

	public void updateScore(
			ScoringBreakdown breakdown,
			int overallScore,
			CandidateGrade grade,
			RiskLevel riskLevel,
			List<String> reasons,
			List<String> warnings
	) {
		if (score == null) {
			assignScore(CandidateScore.create(breakdown, overallScore, grade, riskLevel, reasons, warnings));
			return;
		}
		score.update(breakdown, overallScore, grade, riskLevel, reasons, warnings);
		this.grade = grade;
		this.updatedAt = OffsetDateTime.now();
	}

	public void linkWholesaleProduct(Long wholesaleProductId) {
		this.wholesaleProductId = wholesaleProductId;
	}

	public void saveInterest() {
		this.status = CandidateStatus.SAVED;
	}

	public void exclude() {
		this.status = CandidateStatus.EXCLUDED;
	}

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Keyword getKeyword() {
		return keyword;
	}

	public Long getWholesaleProductId() {
		return wholesaleProductId;
	}

	public CandidateSourceType getSourceType() {
		return sourceType;
	}

	public String getName() {
		return name;
	}

	public CategoryCode getCategoryCode() {
		return categoryCode;
	}

	public int getExpectedSalePrice() {
		return expectedSalePrice;
	}

	public Integer getSupplyPrice() {
		return supplyPrice;
	}

	public Integer getShippingFee() {
		return shippingFee;
	}

	public BigDecimal getExpectedMarginRate() {
		return expectedMarginRate;
	}

	public CandidateGrade getGrade() {
		return grade;
	}

	public CandidateStatus getStatus() {
		return status;
	}

	public CandidateScore getScore() {
		return score;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
