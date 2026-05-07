package com.tibay.tibayai.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tibay.tibayai.entity.AiAssessment;

public interface AiAssessmentRepository extends JpaRepository<AiAssessment, Long> {
	Optional<AiAssessment> findBySubmissionId(Long submissionId);
}

