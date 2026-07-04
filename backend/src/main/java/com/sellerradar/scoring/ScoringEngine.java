package com.sellerradar.scoring;

/**
 * @deprecated Use {@link CandidateScoreCalculator}. This wrapper remains for
 * compatibility with older tests and services that still reference ScoringEngine.
 */
@Deprecated(since = "P6-002", forRemoval = false)
public class ScoringEngine extends CandidateScoreCalculator {
}
