package com.tibay.tibayai.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.tibay.tibayai.repo.NotificationRepository;
import com.tibay.tibayai.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {
	private final UserRepository userRepository;
	private final NotificationRepository notificationRepository;

	@ModelAttribute("currentEmail")
	public String currentEmail() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
			return null;
		}
		return auth.getName();
	}

	@ModelAttribute("unreadNotifCount")
	public Long unreadNotifCount() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
			return 0L;
		}
		var user = userRepository.findByEmail(auth.getName()).orElse(null);
		if (user == null) {
			return 0L;
		}
		return notificationRepository.countByUserIdAndReadAtIsNull(user.getId());
	}
}
