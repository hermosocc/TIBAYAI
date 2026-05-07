package com.tibay.tibayai.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tibay.tibayai.entity.GovernmentIdVerification;

public interface GovernmentIdVerificationRepository extends JpaRepository<GovernmentIdVerification, Long> {
	List<GovernmentIdVerification> findTop5ByWorkerProfileIdOrderByCreatedAtDesc(Long workerProfileId);
}

