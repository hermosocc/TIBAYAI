package com.tibay.tibayai.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tibay.tibayai.entity.WeldingSubmission;

public interface WeldingSubmissionRepository extends JpaRepository<WeldingSubmission, Long> {
	List<WeldingSubmission> findTop10ByWorkerProfileIdOrderBySubmittedAtDesc(Long workerProfileId);
}

