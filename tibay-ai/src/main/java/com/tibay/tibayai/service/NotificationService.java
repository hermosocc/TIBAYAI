package com.tibay.tibayai.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tibay.tibayai.entity.Notification;
import com.tibay.tibayai.entity.NotificationType;
import com.tibay.tibayai.entity.User;
import com.tibay.tibayai.repo.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
	private final NotificationRepository notificationRepository;

	@Transactional
	public void notify(User user, NotificationType type, String message, String link) {
		Notification n = new Notification();
		n.setUser(user);
		n.setType(type);
		n.setMessage(message);
		n.setLink(link);
		notificationRepository.save(n);
	}
}

