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
@Table(name = "ai_assessments")
public class AiAssessment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "submission_id", nullable = false, unique = true)
	private WeldingSubmission submission;

	private Integer skillScore;

	@Column(length = 40)
	private String ppeCompliance;

	@Column(length = 40)
	private String weldingStability;

	@Column(length = 40)
	private String weldLineStraightness;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VerificationStatus authenticityStatus;

	@Column(length = 1200)
	private String authenticitySummary;

	@Column(length = 1200)
	private String assessmentSummary;

	@Column(length = 1200)
	private String recommendations;

	@Column(nullable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (authenticityStatus == null) {
			authenticityStatus = VerificationStatus.PENDING;
		}
	}
}

