package com.tibay.tibayai.controller;

import java.time.Instant;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tibay.tibayai.repo.NotificationRepository;
import com.tibay.tibayai.service.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
	private final CurrentUserService currentUserService;
	private final NotificationRepository notificationRepository;

	@GetMapping
	public String list(Model model) {
		var user = currentUserService.requireUser();
		model.addAttribute("notifications", notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId()));
		return "notifications";
	}

	@PostMapping("/read-all")
	public String readAll() {
		var user = currentUserService.requireUser();
		var list = notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId());
		for (var n : list) {
			if (n.getReadAt() == null) {
				n.setReadAt(Instant.now());
				notificationRepository.save(n);
			}
		}
		return "redirect:/notifications";
	}
}

