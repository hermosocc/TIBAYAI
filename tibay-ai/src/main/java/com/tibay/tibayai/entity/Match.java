package com.tibay.tibayai.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
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
@Table(name = "matches")
public class Match {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "job_post_id", nullable = false)
	private JobPost jobPost;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "worker_profile_id", nullable = false)
	private WorkerProfile workerProfile;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_profile_id", nullable = false)
	private ClientProfile clientProfile;

	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}

