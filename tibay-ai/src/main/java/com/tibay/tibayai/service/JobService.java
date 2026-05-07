package com.tibay.tibayai.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	public JobApplication apply(WorkerProfile worker, JobPost job, String coverNote) {
		if (job.getStatus() != JobStatus.OPEN) {
			throw new IllegalStateException("Job is closed");
		}
		if (jobApplicationRepository.existsByJobPostIdAndWorkerProfileId(job.getId(), worker.getId())) {
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
	public Match hire(ClientProfile client, JobPost job, Long selectedApplicationId) {
		if (!job.getClientProfile().getId().equals(client.getId())) {
			throw new IllegalArgumentException("Not your job post");
		}
		if (job.getStatus() != JobStatus.OPEN) {
			throw new IllegalStateException("Job already closed");
		}

		List<JobApplication> apps = jobApplicationRepository.findByJobPostIdOrderByAppliedAtDesc(job.getId());
		JobApplication selected = apps.stream().filter(a -> a.getId().equals(selectedApplicationId)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Application not found"));

		for (JobApplication a : apps) {
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

