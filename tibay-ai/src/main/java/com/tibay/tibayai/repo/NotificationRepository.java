package com.tibay.tibayai.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tibay.tibayai.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	long countByUserIdAndReadAtIsNull(Long userId);

	List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
}

