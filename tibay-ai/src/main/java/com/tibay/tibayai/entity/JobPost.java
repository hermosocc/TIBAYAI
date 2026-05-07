package com.tibay.tibayai.entity;

import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "job_posts")
public class JobPost {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "client_profile_id", nullable = false)
	private ClientProfile clientProfile;

	@Column(nullable = false, length = 140)
	private String title;

	@Column(nullable = false, length = 2000)
	private String description;

	@Column(nullable = false, length = 120)
	private String specialization;

	@Column(nullable = false)
	private Integer budgetPhp;

	private LocalDate deadline;

	@Column(length = 120)
	private String barangay;

	@Column(length = 120)
	private String city;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private JobStatus status;

	@Column(nullable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (status == null) {
			status = JobStatus.OPEN;
		}
	}
}

