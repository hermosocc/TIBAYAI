package com.tibay.tibayai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tibay.tibayai.dto.JobPostForm;
import com.tibay.tibayai.entity.JobPost;
import com.tibay.tibayai.repo.JobApplicationRepository;
import com.tibay.tibayai.repo.JobPostRepository;
import com.tibay.tibayai.repo.MatchRepository;
import com.tibay.tibayai.repo.WorkerProfileRepository;
import com.tibay.tibayai.service.CurrentUserService;
import com.tibay.tibayai.service.JobService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {
	private final CurrentUserService currentUserService;
	private final JobPostRepository jobPostRepository;
	private final JobApplicationRepository jobApplicationRepository;
	private final WorkerProfileRepository workerProfileRepository;
	private final MatchRepository matchRepository;
	private final JobService jobService;

	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		var client = currentUserService.requireClientProfile();
		model.addAttribute("client", client);
		model.addAttribute("jobs", jobPostRepository.findByClientProfileIdOrderByCreatedAtDesc(client.getId()));
		model.addAttribute("matches", matchRepository.findByClientProfileIdOrderByCreatedAtDesc(client.getId()));
		return "client/dashboard";
	}

	@GetMapping("/jobs/new")
	public String jobNew(Model model) {
		var client = currentUserService.requireClientProfile();
		model.addAttribute("client", client);
		model.addAttribute("form", new JobPostForm());
		return "client/job-new";
	}

	@PostMapping("/jobs/new")
	public String jobNewSubmit(@Valid @ModelAttribute("form") JobPostForm form, BindingResult br, Model model) {
		var client = currentUserService.requireClientProfile();
		model.addAttribute("client", client);
		if (br.hasErrors()) {
			return "client/job-new";
		}
		JobPost job = new JobPost();
		job.setClientProfile(client);
		job.setTitle(form.getTitle());
		job.setDescription(form.getDescription());
		job.setSpecialization(form.getSpecialization());
		job.setBudgetPhp(form.getBudgetPhp());
		job.setDeadline(form.getDeadline());
		job.setBarangay(form.getBarangay());
		job.setCity(form.getCity());
		jobService.createJob(job);
		return "redirect:/client/dashboard";
	}

	@GetMapping("/jobs/{jobId}")
	public String jobView(@PathVariable Long jobId, Model model) {
		var client = currentUserService.requireClientProfile();
		var job = jobPostRepository.findById(jobId).orElseThrow();
		if (!job.getClientProfile().getId().equals(client.getId())) {
			return "redirect:/client/dashboard";
		}
		model.addAttribute("client", client);
		model.addAttribute("job", job);
		model.addAttribute("applications", jobApplicationRepository.findByJobPostIdOrderByAppliedAtDesc(jobId));
		return "client/job-view";
	}

	@PostMapping("/jobs/{jobId}/hire")
	public String hire(@PathVariable Long jobId, @RequestParam("applicationId") Long applicationId) {
		var client = currentUserService.requireClientProfile();
		var job = jobPostRepository.findById(jobId).orElseThrow();
		jobService.hire(client, job, applicationId);
		return "redirect:/client/jobs/" + jobId;
	}

	@GetMapping("/workers/{workerId}")
	public String workerProfile(@PathVariable Long workerId, Model model) {
		var client = currentUserService.requireClientProfile();
		model.addAttribute("client", client);
		var worker = workerProfileRepository.findById(workerId).orElseThrow();
		model.addAttribute("worker", worker);
		return "client/worker-profile";
	}

	@GetMapping("/matches")
	public String matches(Model model) {
		var client = currentUserService.requireClientProfile();
		model.addAttribute("client", client);
		model.addAttribute("matches", matchRepository.findByClientProfileIdOrderByCreatedAtDesc(client.getId()));
		return "client/matches";
	}
}

