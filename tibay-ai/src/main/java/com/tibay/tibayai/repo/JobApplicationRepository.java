package com.tibay.tibayai.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tibay.tibayai.entity.JobApplication;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
	boolean existsByJobPostIdAndWorkerProfileId(Long jobPostId, Long workerProfileId);

	Optional<JobApplication> findByJobPostIdAndWorkerProfileId(Long jobPostId, Long workerProfileId);

	List<JobApplication> findByWorkerProfileIdOrderByAppliedAtDesc(Long workerProfileId);

	List<JobApplication> findByJobPostIdOrderByAppliedAtDesc(Long jobPostId);
}
