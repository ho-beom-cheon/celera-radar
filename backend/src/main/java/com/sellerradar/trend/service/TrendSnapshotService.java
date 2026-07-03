package com.sellerradar.trend.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.domain.ApiCallLog;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import com.sellerradar.common.external.repository.ApiCallLogRepository;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.trend.client.NaverDataLabClient;
import com.sellerradar.trend.client.NaverDataLabKeywordTrendRequest;
import com.sellerradar.trend.client.NaverDataLabKeywordTrendResponse;
import com.sellerradar.trend.client.NaverDataLabTimeUnit;
import com.sellerradar.trend.client.NaverDataLabTrendPoint;
import com.sellerradar.trend.domain.TrendSnapshot;
import com.sellerradar.trend.domain.TrendTimeUnit;
import com.sellerradar.trend.repository.TrendSnapshotRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrendSnapshotService {
	private final KeywordRepository keywordRepository;
	private final TrendSnapshotRepository trendSnapshotRepository;
	private final ApiCallLogRepository apiCallLogRepository;
	private final NaverDataLabClient naverDataLabClient;
	private final TrendScoreCalculator trendScoreCalculator;

	public TrendSnapshotService(
			KeywordRepository keywordRepository,
			TrendSnapshotRepository trendSnapshotRepository,
			ApiCallLogRepository apiCallLogRepository,
			NaverDataLabClient naverDataLabClient,
			TrendScoreCalculator trendScoreCalculator
	) {
		this.keywordRepository = keywordRepository;
		this.trendSnapshotRepository = trendSnapshotRepository;
		this.apiCallLogRepository = apiCallLogRepository;
		this.naverDataLabClient = naverDataLabClient;
		this.trendScoreCalculator = trendScoreCalculator;
	}

	@Transactional
	public TrendSnapshotCollectResult collectKeywordTrend(
			Long keywordId,
			String naverCategoryCode,
			LocalDate startDate,
			LocalDate endDate,
			NaverDataLabTimeUnit timeUnit
	) {
		Keyword keyword = keywordRepository.findById(keywordId)
				.filter(foundKeyword -> foundKeyword.getStatus() == KeywordStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));
		TrendTimeUnit snapshotTimeUnit = TrendTimeUnit.from(timeUnit);
		NaverDataLabKeywordTrendResponse response = requestTrend(
				keyword,
				naverCategoryCode,
				startDate,
				endDate,
				timeUnit
		);
		List<TrendPoint> points = response.results().stream()
				.flatMap(result -> result.data().stream())
				.map(this::toTrendPoint)
				.sorted(Comparator.comparing(TrendPoint::period))
				.toList();
		int savedCount = upsertSnapshots(keyword, startDate, endDate, snapshotTimeUnit, points);
		TrendScoreResult score = trendScoreCalculator.calculate(points);
		return new TrendSnapshotCollectResult(keyword.getId(), savedCount, score, points);
	}

	@Transactional(readOnly = true)
	public TrendScoreResult calculateSavedTrendScore(Long keywordId, TrendTimeUnit timeUnit) {
		List<TrendPoint> points = trendSnapshotRepository.findByKeyword_IdAndTimeUnitOrderByDataPeriodAsc(keywordId, timeUnit)
				.stream()
				.map(snapshot -> new TrendPoint(snapshot.getDataPeriod(), snapshot.getRatio()))
				.toList();
		return trendScoreCalculator.calculate(points);
	}

	private NaverDataLabKeywordTrendResponse requestTrend(
			Keyword keyword,
			String naverCategoryCode,
			LocalDate startDate,
			LocalDate endDate,
			NaverDataLabTimeUnit timeUnit
	) {
		try {
			NaverDataLabKeywordTrendResponse response = naverDataLabClient.searchKeywordTrend(
					new NaverDataLabKeywordTrendRequest(
							startDate,
							endDate,
							timeUnit,
							naverCategoryCode,
							keyword.getKeyword()
					)
			);
			apiCallLogRepository.save(ApiCallLog.success(
					ExternalApiProvider.NAVER_DATALAB,
					NaverDataLabClient.KEYWORD_TREND_API_NAME,
					keyword.getId(),
					endDate
			));
			return response;
		} catch (RuntimeException exception) {
			recordApiFailure(keyword, endDate, exception);
			throw exception;
		}
	}

	private void recordApiFailure(Keyword keyword, LocalDate baseDate, RuntimeException exception) {
		ErrorCode errorCode = exception instanceof BusinessException businessException
				? businessException.errorCode()
				: ErrorCode.EXTERNAL_API_UNAVAILABLE;
		apiCallLogRepository.save(ApiCallLog.failure(
				ExternalApiProvider.NAVER_DATALAB,
				NaverDataLabClient.KEYWORD_TREND_API_NAME,
				keyword.getId(),
				baseDate,
				errorCode.status().value(),
				errorCode.name(),
				exception.getMessage()
		));
	}

	private int upsertSnapshots(
			Keyword keyword,
			LocalDate startDate,
			LocalDate endDate,
			TrendTimeUnit timeUnit,
			List<TrendPoint> points
	) {
		int savedCount = 0;
		for (TrendPoint point : points) {
			TrendSnapshot snapshot = trendSnapshotRepository
					.findByKeyword_IdAndSnapshotDateAndDataPeriodAndTimeUnit(
							keyword.getId(),
							endDate,
							point.period(),
							timeUnit
					)
					.map(existingSnapshot -> {
						existingSnapshot.updateRatio(point.ratio());
						return existingSnapshot;
					})
					.orElseGet(() -> TrendSnapshot.create(
							keyword,
							endDate,
							startDate,
							endDate,
							point.period(),
							timeUnit,
							point.ratio(),
							null
					));
			trendSnapshotRepository.save(snapshot);
			savedCount++;
		}
		return savedCount;
	}

	private TrendPoint toTrendPoint(NaverDataLabTrendPoint point) {
		return new TrendPoint(LocalDate.parse(point.period()), point.ratio());
	}
}
