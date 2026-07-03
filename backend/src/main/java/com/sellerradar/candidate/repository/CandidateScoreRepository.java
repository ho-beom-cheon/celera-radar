package com.sellerradar.candidate.repository;

import com.sellerradar.candidate.domain.CandidateScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateScoreRepository extends JpaRepository<CandidateScore, Long> {
}
