package com.tibay.tibayai.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tibay.tibayai.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);
}

