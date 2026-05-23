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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "worker_profiles")
public class WorkerProfile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(nullable = false, length = 120)
	private String fullName;

	@Column(length = 50)
	private String phone;

	@Column(length = 120)
	private String barangay;

	@Column(length = 120)
	private String city;

	@Column(length = 300)
	private String selfiePath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VerificationStatus idVerificationStatus;

	@Column(length = 800)
	private String idVerificationSummary;

	@Column(length = 300)
	private String latestSubmissionMediaPath;

	private Integer latestSkillScore;

	private Integer finalRatingScore;

	@Column(length = 4000)
	private String atsPortfolio;

	@Column(nullable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (idVerificationStatus == null) {
			idVerificationStatus = VerificationStatus.PENDING;
		}
	}
}
