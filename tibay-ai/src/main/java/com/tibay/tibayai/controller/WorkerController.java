package com.tibay.tibayai.controller;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.tibay.tibayai.dto.ApplyForm;
import com.tibay.tibayai.dto.PortfolioForm;
import com.tibay.tibayai.entity.JobStatus;
import com.tibay.tibayai.entity.MediaType;
import com.tibay.tibayai.entity.VerificationStatus;
import com.tibay.tibayai.entity.WeldingSubmission;
import com.tibay.tibayai.repo.AiAssessmentRepository;
import com.tibay.tibayai.repo.JobApplicationRepository;
import com.tibay.tibayai.repo.JobPostRepository;
import com.tibay.tibayai.repo.MatchRepository;
import com.tibay.tibayai.repo.WeldingSubmissionRepository;
import com.tibay.tibayai.service.AssessmentService;
import com.tibay.tibayai.service.CurrentUserService;
import com.tibay.tibayai.service.JobService;
import com.tibay.tibayai.service.PortfolioService;
import com.tibay.tibayai.service.StorageService;
import com.tibay.tibayai.service.VerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/worker")
@RequiredArgsConstructor
public class WorkerController {
	private final CurrentUserService currentUserService;
	private final StorageService storageService;
	private final VerificationService verificationService;
	private final JobPostRepository jobPostRepository;
	private final JobApplicationRepository jobApplicationRepository;
	private final JobService jobService;
	private final MatchRepository matchRepository;
	private final WeldingSubmissionRepository weldingSubmissionRepository;
	private final AiAssessmentRepository aiAssessmentRepository;
	private final AssessmentService assessmentService;
	private final PortfolioService portfolioService;

	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		var worker = currentUserService.requireWorkerProfile();
		model.addAttribute("worker", worker);
		model.addAttribute("jobs", jobPostRepository.findNearbyOpenJobs(JobStatus.OPEN, worker.getCity(), worker.getBarangay()));
		return "worker/dashboard";
	}

	@GetMapping("/verify")
	public String verifyForm(Model model) {
		var worker = currentUserService.requireWorkerProfile();
		model.addAttribute("worker", worker);
		return "worker/verify";
	}

	@PostMapping("/verify")
	public String verifySubmit(@RequestParam("govId") MultipartFile govId, @RequestParam("selfie") MultipartFile selfie, Model model)
			throws IOException {
		var worker = currentUserService.requireWorkerProfile();
		if (govId == null || govId.isEmpty() || selfie == null || selfie.isEmpty()) {
			model.addAttribute("worker", worker);
			model.addAttribute("error", "Government ID and selfie are required.");
			return "worker/verify";
		}
		String idPath = storageService.store(govId, "worker/" + worker.getId() + "/id");
		String selfiePath = storageService.store(selfie, "worker/" + worker.getId() + "/selfie");
		worker.setSelfiePath(selfiePath);
		verificationService.verify(worker, idPath, selfiePath);
		return "redirect:/worker/profile";
	}

	@GetMapping("/profile")
	public String profile(Model model) {
		var worker = currentUserService.requireWorkerProfile();
		model.addAttribute("worker", worker);
		var subs = weldingSubmissionRepository.findTop10ByWorkerProfileIdOrderBySubmittedAtDesc(worker.getId());
		model.addAttribute("submissions", subs);
		if (!subs.isEmpty()) {
			var a = aiAssessmentRepository.findBySubmissionId(subs.get(0).getId()).orElse(null);
			model.addAttribute("latestAssessment", a);
		}
		return "worker/profile";
	}

	@GetMapping("/submission")
	public String submissionForm(Model model) {
		var worker = currentUserService.requireWorkerProfile();
		if (worker.getIdVerificationStatus() != VerificationStatus.VERIFIED) {
			return "redirect:/worker/verify";
		}
		model.addAttribute("worker", worker);
		return "worker/submission";
	}

	@PostMapping("/submission")
	public String submissionSubmit(@RequestParam("media") MultipartFile media, @RequestParam("mediaType") String mediaType, Model model)
			throws IOException {
		var worker = currentUserService.requireWorkerProfile();
		if (worker.getIdVerificationStatus() != VerificationStatus.VERIFIED) {
			return "redirect:/worker/verify";
		}
		if (media == null || media.isEmpty()) {
			model.addAttribute("worker", worker);
			model.addAttribute("error", "Please upload an image or video.");
			return "worker/submission";
		}
		MediaType mt = "VIDEO".equalsIgnoreCase(mediaType) ? MediaType.VIDEO : MediaType.IMAGE;
		String mediaPath = storageService.store(media, "worker/" + worker.getId() + "/weld");

		WeldingSubmission sub = new WeldingSubmission();
		sub.setWorkerProfile(worker);
		sub.setMediaType(mt);
		sub.setMediaPath(mediaPath);
		weldingSubmissionRepository.save(sub);

		assessmentService.assess(worker, sub);
		return "redirect:/worker/profile";
	}

	@GetMapping("/jobs/{jobId}")
	public String jobView(@PathVariable Long jobId, Model model) {
		var worker = currentUserService.requireWorkerProfile();
		var job = jobPostRepository.findById(jobId).orElseThrow();
		model.addAttribute("worker", worker);
		model.addAttribute("job", job);
		model.addAttribute("alreadyApplied", jobApplicationRepository.existsByJobPostIdAndWorkerProfileId(jobId, worker.getId()));
		model.addAttribute("form", new ApplyForm());
		return "worker/job-view";
	}

	@PostMapping("/jobs/{jobId}/apply")
	public String jobApply(@PathVariable Long jobId, @Valid @ModelAttribute("form") ApplyForm form, BindingResult br, Model model) {
		var worker = currentUserService.requireWorkerProfile();
		if (worker.getIdVerificationStatus() != VerificationStatus.VERIFIED) {
			return "redirect:/worker/verify";
		}
		var job = jobPostRepository.findById(jobId).orElseThrow();
		if (br.hasErrors()) {
			model.addAttribute("worker", worker);
			model.addAttribute("job", job);
			model.addAttribute("alreadyApplied", false);
			return "worker/job-view";
		}
		jobService.apply(worker, job, form.getCoverNote());
		return "redirect:/worker/applications";
	}

	@GetMapping("/applications")
	public String applications(Model model) {
		var worker = currentUserService.requireWorkerProfile();
		model.addAttribute("worker", worker);
		model.addAttribute("applications", jobApplicationRepository.findByWorkerProfileIdOrderByAppliedAtDesc(worker.getId()));
		return "worker/applications";
	}

	@GetMapping("/matches")
	public String matches(Model model) {
		var worker = currentUserService.requireWorkerProfile();
		model.addAttribute("worker", worker);
		model.addAttribute("matches", matchRepository.findByWorkerProfileIdOrderByCreatedAtDesc(worker.getId()));
		return "worker/matches";
	}

	@GetMapping("/portfolio")
	public String portfolioForm(Model model) {
		var worker = currentUserService.requireWorkerProfile();
		model.addAttribute("worker", worker);
		var subs = weldingSubmissionRepository.findTop10ByWorkerProfileIdOrderBySubmittedAtDesc(worker.getId());
		var assessment = !subs.isEmpty() ? aiAssessmentRepository.findBySubmissionId(subs.get(0).getId()).orElse(null) : null;
		PortfolioForm form = new PortfolioForm();
		if (assessment != null) {
			form.setCvResults(assessment.getAssessmentSummary());
		}
		model.addAttribute("form", form);
		return "worker/portfolio";
	}

	@PostMapping("/portfolio")
	public String portfolioSubmit(@Valid @ModelAttribute("form") PortfolioForm form, BindingResult br, Model model) {
		var worker = currentUserService.requireWorkerProfile();
		model.addAttribute("worker", worker);
		if (br.hasErrors()) {
			return "worker/portfolio";
		}
		var subs = weldingSubmissionRepository.findTop10ByWorkerProfileIdOrderBySubmittedAtDesc(worker.getId());
		var assessment = !subs.isEmpty() ? aiAssessmentRepository.findBySubmissionId(subs.get(0).getId()).orElse(null) : null;
		String portfolio = portfolioService.generateAndSave(worker, form.getRawText(), assessment, form.getCvResults());
		model.addAttribute("portfolioGenerated", true);
		model.addAttribute("portfolio", portfolio);
		return "worker/portfolio";
	}
}
