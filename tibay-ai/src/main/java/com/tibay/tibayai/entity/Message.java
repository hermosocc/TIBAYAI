package com.tibay.tibayai.entity;

import java.time.Instant;

import jakarta.persistence.Column;
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
@Table(name = "messages")
public class Message {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", nullable = false)
	private Match match;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_user_id", nullable = false)
	private User senderUser;

	@Column(nullable = false, length = 2000)
	private String body;

	@Column(nullable = false)
	private Instant sentAt;

	@PrePersist
	void prePersist() {
		if (sentAt == null) {
			sentAt = Instant.now();
		}
	}
}

