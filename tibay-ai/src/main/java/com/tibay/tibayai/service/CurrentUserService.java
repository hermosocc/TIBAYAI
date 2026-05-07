package com.tibay.tibayai.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.tibay.tibayai.entity.ClientProfile;
import com.tibay.tibayai.entity.Role;
import com.tibay.tibayai.entity.User;
import com.tibay.tibayai.entity.WorkerProfile;
import com.tibay.tibayai.repo.ClientProfileRepository;
import com.tibay.tibayai.repo.UserRepository;
import com.tibay.tibayai.repo.WorkerProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
	private final UserRepository userRepository;
	private final WorkerProfileRepository workerProfileRepository;
	private final ClientProfileRepository clientProfileRepository;

	public User requireUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {
			throw new IllegalStateException("Not authenticated");
		}
		return userRepository.findByEmail(auth.getName()).orElseThrow();
	}

	public WorkerProfile requireWorkerProfile() {
		User u = requireUser();
		if (u.getRole() != Role.WORKER) {
			throw new IllegalStateException("Not a worker");
		}
		return workerProfileRepository.findByUserId(u.getId()).orElseThrow();
	}

	public ClientProfile requireClientProfile() {
		User u = requireUser();
		if (u.getRole() != Role.CLIENT) {
			throw new IllegalStateException("Not a client");
		}
		return clientProfileRepository.findByUserId(u.getId()).orElseThrow();
	}
}

