package com.tibay.tibayai.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tibay.tibayai.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {
	List<Match> findByWorkerProfileIdOrderByCreatedAtDesc(Long workerProfileId);

	List<Match> findByClientProfileIdOrderByCreatedAtDesc(Long clientProfileId);
}

