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
@Table(name = "gov_id_verifications")
public class GovernmentIdVerification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "worker_profile_id", nullable = false)
	private WorkerProfile workerProfile;

	@Column(nullable = false, length = 300)
	private String idImagePath;

	@Column(length = 4000)
	private String ocrText;

	private Integer faceMatchScore;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VerificationStatus status;

	@Column(length = 1200)
	private String summary;

	@Column(nullable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (status == null) {
			status = VerificationStatus.PENDING;
		}
	}
}

