package com.tibay.tibayai.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tibay.tibayai.entity.JobPost;
import com.tibay.tibayai.entity.JobStatus;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {
	List<JobPost> findByClientProfileIdOrderByCreatedAtDesc(Long clientProfileId);

	@Query("""
			select j from JobPost j
			where j.status = :status
			  and (:city is null or lower(j.city) = lower(:city))
			  and (:barangay is null or lower(j.barangay) = lower(:barangay))
			order by
			  case when lower(j.barangay) = lower(:barangay) then 0 else 1 end,
			  j.createdAt desc
			""")
	List<JobPost> findNearbyOpenJobs(
			@Param("status") JobStatus status,
			@Param("city") String city,
			@Param("barangay") String barangay);
}

