package com.tibay.tibayai.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
		String emailNorm = normalize(email);
		String fullNameNorm = normalize(fullName);
		String phoneNorm = normalize(phone);
		String barangayNorm = normalize(barangay);
		String cityNorm = normalize(city);

		User u = new User();
		u.setEmail(emailNorm);
		u.setPasswordHash(passwordEncoder.encode(rawPassword));
		u.setRole(Role.WORKER);
		userRepository.save(u);

		WorkerProfile profile = new WorkerProfile();
		profile.setUser(u);
		profile.setFullName(fullNameNorm);
		profile.setPhone(phoneNorm);
		profile.setBarangay(barangayNorm);
		profile.setCity(cityNorm);
		profile.setIdVerificationStatus(VerificationStatus.PENDING);
		workerProfileRepository.save(profile);
		return u;
	}

	@Transactional
	public User registerClient(String email, String rawPassword, String companyName, String contactName, String phone, String barangay, String city) {
		String emailNorm = normalize(email);
		String companyNorm = normalize(companyName);
		String contactNorm = normalize(contactName);
		String phoneNorm = normalize(phone);
		String barangayNorm = normalize(barangay);
		String cityNorm = normalize(city);

		User u = new User();
		u.setEmail(emailNorm);
		u.setPasswordHash(passwordEncoder.encode(rawPassword));
		u.setRole(Role.CLIENT);
		userRepository.save(u);

		ClientProfile profile = new ClientProfile();
		profile.setUser(u);
		profile.setCompanyName(companyNorm);
		profile.setContactName(contactNorm);
		profile.setPhone(phoneNorm);
		profile.setBarangay(barangayNorm);
		profile.setCity(cityNorm);
		clientProfileRepository.save(profile);
		return u;
	}

	private static String normalize(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
