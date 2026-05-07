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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "welding_submissions")
public class WeldingSubmission {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "worker_profile_id", nullable = false)
	private WorkerProfile workerProfile;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private MediaType mediaType;

	@Column(nullable = false, length = 300)
	private String mediaPath;

	@Column(nullable = false)
	private Instant submittedAt;

	@OneToOne(mappedBy = "submission", fetch = FetchType.LAZY)
	private AiAssessment assessment;

	@PrePersist
	void prePersist() {
		if (submittedAt == null) {
			submittedAt = Instant.now();
		}
	}
}

