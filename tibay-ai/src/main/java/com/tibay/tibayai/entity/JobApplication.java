package com.tibay.tibayai.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "job_applications")
public class JobApplication {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_post_id", nullable = false)
	private JobPost jobPost;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "worker_profile_id", nullable = false)
	private WorkerProfile workerProfile;

	@Column(length = 1200)
	private String coverNote;

	private Integer aiScore;

	private Integer hirerScore;

	private Integer finalScore;

	private Boolean hirerOverride;

	@Column(length = 800)
	private String hirerNotes;

	private Instant scoredAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApplicationStatus status;

	@Column(nullable = false)
	private Instant appliedAt;

	@PrePersist
	void prePersist() {
		if (appliedAt == null) {
			appliedAt = Instant.now();
		}
		if (status == null) {
			status = ApplicationStatus.APPLIED;
		}
	}
}
