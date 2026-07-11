package com.sellerradar.candidate.domain;

import com.sellerradar.scoring.CandidateGrade;
import com.sellerradar.scoring.ScoringBreakdown;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "candidate_score")
public class CandidateScore {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "candidate_id", nullable = false, unique = true)
	private ProductCandidate candidate;

	@Column(name = "trend_score", nullable = false)
	private int trendScore;

	@Column(name = "competition_score", nullable = false)
	private int competitionScore;

	@Column(name = "margin_score", nullable = false)
	private int marginScore;

	@Column(name = "price_band_score", nullable = false)
	private int priceBandScore;

	@Column(name = "supply_score", nullable = false)
	private int supplyScore;

	@Column(name = "risk_penalty", nullable = false)
	private int riskPenalty;

	@Column(name = "overall_score", nullable = false)
	private int overallScore;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CandidateGrade grade;

	@Enumerated(EnumType.STRING)
	@Column(name = "risk_level", nullable = false, length = 20)
	private RiskLevel riskLevel;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String reasons;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String warnings;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected CandidateScore() {
	}

	private CandidateScore(
			ScoringBreakdown breakdown,
			int overallScore,
			CandidateGrade grade,
			RiskLevel riskLevel,
			List<String> reasons,
			List<String> warnings
	) {
		this.trendScore = breakdown.trendScore();
		this.competitionScore = breakdown.competitionScore();
		this.marginScore = breakdown.marginScore();
		this.priceBandScore = breakdown.priceBandScore();
		this.supplyScore = breakdown.supplyScore();
		this.riskPenalty = breakdown.riskPenalty();
		this.overallScore = overallScore;
		this.grade = grade;
		this.riskLevel = riskLevel;
		this.reasons = joinLines(reasons);
		this.warnings = joinLines(warnings);
	}

	public static CandidateScore create(
			ScoringBreakdown breakdown,
			int overallScore,
			CandidateGrade grade,
			RiskLevel riskLevel,
			List<String> reasons,
			List<String> warnings
	) {
		return new CandidateScore(breakdown, overallScore, grade, riskLevel, reasons, warnings);
	}

	void attachTo(ProductCandidate candidate) {
		this.candidate = candidate;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	public ScoringBreakdown breakdown() {
		return new ScoringBreakdown(
				trendScore,
				competitionScore,
				marginScore,
				priceBandScore,
				supplyScore,
				riskPenalty
		);
	}

	public List<String> reasons() {
		return splitLines(reasons);
	}

	public List<String> warnings() {
		return splitLines(warnings);
	}

	private static String joinLines(List<String> values) {
		if (values == null || values.isEmpty()) {
			return "";
		}
		return values.stream()
				.filter(Objects::nonNull)
				.map(String::strip)
				.filter(value -> !value.isBlank())
				.collect(Collectors.joining("\n"));
	}

	private static List<String> splitLines(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return value.lines()
				.map(String::strip)
				.filter(line -> !line.isBlank())
				.toList();
	}

	public Long getId() {
		return id;
	}

	public ProductCandidate getCandidate() {
		return candidate;
	}

	public int getTrendScore() {
		return trendScore;
	}

	public int getCompetitionScore() {
		return competitionScore;
	}

	public int getMarginScore() {
		return marginScore;
	}

	public int getPriceBandScore() {
		return priceBandScore;
	}

	public int getSupplyScore() {
		return supplyScore;
	}

	public int getRiskPenalty() {
		return riskPenalty;
	}

	public int getOverallScore() {
		return overallScore;
	}

	public CandidateGrade getGrade() {
		return grade;
	}

	public RiskLevel getRiskLevel() {
		return riskLevel;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
