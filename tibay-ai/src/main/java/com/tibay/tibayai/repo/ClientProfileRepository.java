package com.tibay.tibayai.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tibay.tibayai.entity.ClientProfile;

public interface ClientProfileRepository extends JpaRepository<ClientProfile, Long> {
	Optional<ClientProfile> findByUserId(Long userId);
}

