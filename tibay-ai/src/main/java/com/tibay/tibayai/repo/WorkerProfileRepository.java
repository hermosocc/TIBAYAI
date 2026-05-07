package com.tibay.tibayai.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tibay.tibayai.entity.WorkerProfile;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {
	Optional<WorkerProfile> findByUserId(Long userId);
}

