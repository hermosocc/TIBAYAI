package com.tibay.tibayai.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.tibay.tibayai.entity.ClientProfile;
import com.tibay.tibayai.entity.JobPost;
import com.tibay.tibayai.entity.Role;
import com.tibay.tibayai.entity.User;
import com.tibay.tibayai.entity.VerificationStatus;
import com.tibay.tibayai.entity.WorkerProfile;
import com.tibay.tibayai.repo.ClientProfileRepository;
import com.tibay.tibayai.repo.JobPostRepository;
import com.tibay.tibayai.repo.UserRepository;
import com.tibay.tibayai.repo.WorkerProfileRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {
	private final TibayProperties properties;
	private final UserRepository userRepository;
	private final WorkerProfileRepository workerProfileRepository;
	private final ClientProfileRepository clientProfileRepository;
	private final JobPostRepository jobPostRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) {
		if (!properties.isSeedDemo()) {
			return;
		}
		if (userRepository.count() > 0) {
			return;
		}

		User clientUser = new User();
		clientUser.setEmail("client@demo.com");
		clientUser.setPasswordHash(passwordEncoder.encode("demo1234"));
		clientUser.setRole(Role.CLIENT);
		userRepository.save(clientUser);

		ClientProfile client = new ClientProfile();
		client.setUser(clientUser);
		client.setCompanyName("Cebu Shipworks");
		client.setContactName("Maria Santos");
		client.setPhone("0917-000-0000");
		client.setBarangay("San Roque");
		client.setCity("Cebu City");
		clientProfileRepository.save(client);

		User workerUser = new User();
		workerUser.setEmail("worker@demo.com");
		workerUser.setPasswordHash(passwordEncoder.encode("demo1234"));
		workerUser.setRole(Role.WORKER);
		userRepository.save(workerUser);

		WorkerProfile worker = new WorkerProfile();
		worker.setUser(workerUser);
		worker.setFullName("Juan Dela Cruz");
		worker.setPhone("0908-000-0000");
		worker.setBarangay("San Roque");
		worker.setCity("Cebu City");
		worker.setIdVerificationStatus(VerificationStatus.VERIFIED);
		worker.setIdVerificationSummary(
				"AI-assisted identity verification: VERIFIED (demo-seeded). This is not a legal verification.");
		workerProfileRepository.save(worker);

		jobPostRepository.save(buildJob(client, "Maritime Hull Repair Welder",
				"Perform hull crack repair and reinforcement welding for docked vessels. Familiarity with shipyard safety rules preferred.",
				"Maritime hull repair / SMAW", 25000, "San Roque", "Cebu City"));
		jobPostRepository.save(buildJob(client, "Structural Welding Helper",
				"Assist in structural welding, joint preparation, and basic QC checks for industrial maintenance projects.",
				"Structural welding / heavy maintenance", 18000, "Mabolo", "Cebu City"));
		jobPostRepository.save(buildJob(client, "Offshore Maintenance Welder (On-call)",
				"Support offshore energy maintenance tasks, including weld repair, equipment support, and safety documentation.",
				"Offshore energy / maintenance welding", 32000, "Poblacion", "Lapu-Lapu City"));
	}

	private JobPost buildJob(ClientProfile client, String title, String desc, String spec, int budget, String barangay, String city) {
		JobPost j = new JobPost();
		j.setClientProfile(client);
		j.setTitle(title);
		j.setDescription(desc);
		j.setSpecialization(spec);
		j.setBudgetPhp(budget);
		j.setBarangay(barangay);
		j.setCity(city);
		return j;
	}
}
