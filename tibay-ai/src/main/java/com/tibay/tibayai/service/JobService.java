package com.tibay.tibayai.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tibay.tibayai.dto.JobPostForm;
import com.tibay.tibayai.entity.ApplicationStatus;
import com.tibay.tibayai.entity.ClientProfile;
import com.tibay.tibayai.entity.JobApplication;
import com.tibay.tibayai.entity.JobPost;
import com.tibay.tibayai.entity.JobStatus;
import com.tibay.tibayai.entity.Match;
import com.tibay.tibayai.entity.NotificationType;
import com.tibay.tibayai.entity.WorkerProfile;
import com.tibay.tibayai.repo.JobApplicationRepository;
import com.tibay.tibayai.repo.JobPostRepository;
import com.tibay.tibayai.repo.MatchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobService {
	private final JobPostRepository jobPostRepository;
	private final JobApplicationRepository jobApplicationRepository;
	private final MatchRepository matchRepository;
	private final NotificationService notificationService;

	@Transactional
	public JobPost createJob(JobPost jobPost) {
		return jobPostRepository.save(jobPost);
	}

	@Transactional
	public JobPost updateJob(ClientProfile client, JobPost job, JobPostForm form) {
		if (!job.getClientProfile().getId().equals(client.getId())) {
			throw new IllegalArgumentException("Not your job post");
		}
		if (job.getStatus() != JobStatus.OPEN) {
			throw new IllegalStateException("Job is closed");
		}
		job.setTitle(form.getTitle());
		job.setDescription(form.getDescription());
		job.setSpecialization(form.getSpecialization());
		job.setBudgetPhp(form.getBudgetPhp());
		job.setDeadline(form.getDeadline());
		job.setBarangay(form.getBarangay());
		job.setCity(form.getCity());
		return jobPostRepository.save(job);
	}

	@Transactional
	public void deleteJob(ClientProfile client, JobPost job) {
		if (!job.getClientProfile().getId().equals(client.getId())) {
			throw new IllegalArgumentException("Not your job post");
		}
		if (!jobApplicationRepository.findByJobPostIdOrderByAppliedAtDesc(job.getId()).isEmpty()) {
			throw new IllegalStateException("Job has applications");
		}
		jobPostRepository.delete(job);
	}

	@Transactional
	public JobApplication apply(WorkerProfile worker, JobPost job, String coverNote) {
		if (job.getStatus() != JobStatus.OPEN) {
			throw new IllegalStateException("Job is closed");
		}
		var existingOpt = jobApplicationRepository.findByJobPostIdAndWorkerProfileId(job.getId(), worker.getId());
		if (existingOpt.isPresent()) {
			JobApplication existing = existingOpt.get();
			if (existing.getStatus() == ApplicationStatus.WITHDRAWN) {
				existing.setStatus(ApplicationStatus.APPLIED);
				existing.setAppliedAt(Instant.now());
				existing.setCoverNote(coverNote);
				jobApplicationRepository.save(existing);
				notificationService.notify(job.getClientProfile().getUser(), NotificationType.APPLICATION_RECEIVED,
						"New application received for: " + job.getTitle(), "/client/jobs/" + job.getId());
				return existing;
			}
			throw new IllegalStateException("Already applied");
		}
		JobApplication app = new JobApplication();
		app.setJobPost(job);
		app.setWorkerProfile(worker);
		app.setCoverNote(coverNote);
		jobApplicationRepository.save(app);

		notificationService.notify(job.getClientProfile().getUser(), NotificationType.APPLICATION_RECEIVED,
				"New application received for: " + job.getTitle(), "/client/jobs/" + job.getId());
		return app;
	}

	@Transactional
	public JobApplication withdrawApplication(WorkerProfile worker, Long applicationId) {
		JobApplication app = jobApplicationRepository.findById(applicationId).orElseThrow();
		if (!app.getWorkerProfile().getId().equals(worker.getId())) {
			throw new IllegalArgumentException("Not your application");
		}
		if (app.getStatus() != ApplicationStatus.APPLIED) {
			throw new IllegalStateException("Cannot withdraw");
		}
		app.setStatus(ApplicationStatus.WITHDRAWN);
		jobApplicationRepository.save(app);
		notificationService.notify(app.getJobPost().getClientProfile().getUser(), NotificationType.APPLICATION_STATUS_CHANGED,
				"Application withdrawn for " + app.getJobPost().getTitle(), "/client/jobs/" + app.getJobPost().getId());
		return app;
	}

	@Transactional
	public JobApplication rejectApplication(ClientProfile client, JobPost job, Long applicationId) {
		if (!job.getClientProfile().getId().equals(client.getId())) {
			throw new IllegalArgumentException("Not your job post");
		}
		if (job.getStatus() != JobStatus.OPEN) {
			throw new IllegalStateException("Job is closed");
		}
		JobApplication app = jobApplicationRepository.findById(applicationId).orElseThrow();
		if (!app.getJobPost().getId().equals(job.getId())) {
			throw new IllegalArgumentException("Application not found");
		}
		if (app.getStatus() != ApplicationStatus.APPLIED) {
			throw new IllegalStateException("Cannot reject");
		}
		app.setStatus(ApplicationStatus.REJECTED);
		jobApplicationRepository.save(app);
		notificationService.notify(app.getWorkerProfile().getUser(), NotificationType.APPLICATION_STATUS_CHANGED,
				"Application update for " + job.getTitle() + ": " + app.getStatus(), "/worker/applications");
		return app;
	}

	@Transactional
	public Match hire(ClientProfile client, JobPost job, Long selectedApplicationId) {
		if (!job.getClientProfile().getId().equals(client.getId())) {
			throw new IllegalArgumentException("Not your job post");
		}
		if (job.getStatus() != JobStatus.OPEN) {
			throw new IllegalStateException("Job already closed");
		}

		List<JobApplication> apps = jobApplicationRepository.findByJobPostIdOrderByAppliedAtDesc(job.getId());
		JobApplication selected = apps.stream()
				.filter(a -> a.getId().equals(selectedApplicationId) && a.getStatus() == ApplicationStatus.APPLIED)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Application not found"));

		for (JobApplication a : apps) {
			if (a.getStatus() == ApplicationStatus.WITHDRAWN) {
				continue;
			}
			if (a.getId().equals(selected.getId())) {
				a.setStatus(ApplicationStatus.HIRED);
			} else {
				a.setStatus(ApplicationStatus.REJECTED);
			}
			jobApplicationRepository.save(a);
			notificationService.notify(a.getWorkerProfile().getUser(), NotificationType.APPLICATION_STATUS_CHANGED,
					"Application update for " + job.getTitle() + ": " + a.getStatus(), "/worker/applications");
		}

		job.setStatus(JobStatus.CLOSED);
		jobPostRepository.save(job);

		Match match = new Match();
		match.setJobPost(job);
		match.setClientProfile(client);
		match.setWorkerProfile(selected.getWorkerProfile());
		matchRepository.save(match);

		notificationService.notify(selected.getWorkerProfile().getUser(), NotificationType.HIRED,
				"You were selected for: " + job.getTitle(), "/chat/" + match.getId());
		return match;
	}
}
