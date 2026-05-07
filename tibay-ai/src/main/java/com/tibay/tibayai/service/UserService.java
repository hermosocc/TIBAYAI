package com.tibay.tibayai.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tibay.tibayai.entity.ClientProfile;
import com.tibay.tibayai.entity.Role;
import com.tibay.tibayai.entity.User;
import com.tibay.tibayai.entity.VerificationStatus;
import com.tibay.tibayai.entity.WorkerProfile;
import com.tibay.tibayai.repo.ClientProfileRepository;
import com.tibay.tibayai.repo.UserRepository;
import com.tibay.tibayai.repo.WorkerProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final WorkerProfileRepository workerProfileRepository;
	private final ClientProfileRepository clientProfileRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public User registerWorker(String email, String rawPassword, String fullName, String phone, String barangay, String city) {
		User u = new User();
		u.setEmail(email);
		u.setPasswordHash(passwordEncoder.encode(rawPassword));
		u.setRole(Role.WORKER);
		userRepository.save(u);

		WorkerProfile p = new WorkerProfile();
		p.setUser(u);
		p.setFullName(fullName);
		p.setPhone(phone);
		p.setBarangay(barangay);
		p.setCity(city);
		p.setIdVerificationStatus(VerificationStatus.PENDING);
		workerProfileRepository.save(p);
		return u;
	}

	@Transactional
	public User registerClient(String email, String rawPassword, String companyName, String contactName, String phone, String barangay, String city) {
		User u = new User();
		u.setEmail(email);
		u.setPasswordHash(passwordEncoder.encode(rawPassword));
		u.setRole(Role.CLIENT);
		userRepository.save(u);

		ClientProfile c = new ClientProfile();
		c.setUser(u);
		c.setCompanyName(companyName);
		c.setContactName(contactName);
		c.setPhone(phone);
		c.setBarangay(barangay);
		c.setCity(city);
		clientProfileRepository.save(c);
		return u;
	}
}

