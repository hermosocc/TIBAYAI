package com.tibay.tibayai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tibay.tibayai.dto.MessageForm;
import com.tibay.tibayai.repo.MatchRepository;
import com.tibay.tibayai.repo.MessageRepository;
import com.tibay.tibayai.service.ChatService;
import com.tibay.tibayai.service.CurrentUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
	private final CurrentUserService currentUserService;
	private final MatchRepository matchRepository;
	private final MessageRepository messageRepository;
	private final ChatService chatService;

	@GetMapping("/{matchId}")
	public String thread(@PathVariable Long matchId, Model model) {
		var user = currentUserService.requireUser();
		var match = matchRepository.findById(matchId).orElseThrow();
		if (!match.getClientProfile().getUser().getId().equals(user.getId())
				&& !match.getWorkerProfile().getUser().getId().equals(user.getId())) {
			return "redirect:/";
		}
		model.addAttribute("match", match);
		model.addAttribute("messages", messageRepository.findByMatchIdOrderBySentAtAsc(matchId));
		model.addAttribute("form", new MessageForm());
		return "chat/thread";
	}

	@PostMapping("/{matchId}")
	public String send(@PathVariable Long matchId, @Valid @ModelAttribute("form") MessageForm form, BindingResult br, Model model) {
		var user = currentUserService.requireUser();
		var match = matchRepository.findById(matchId).orElseThrow();
		if (!match.getClientProfile().getUser().getId().equals(user.getId())
				&& !match.getWorkerProfile().getUser().getId().equals(user.getId())) {
			return "redirect:/";
		}
		if (br.hasErrors()) {
			model.addAttribute("match", match);
			model.addAttribute("messages", messageRepository.findByMatchIdOrderBySentAtAsc(matchId));
			return "chat/thread";
		}
		chatService.send(match, user, form.getBody());
		return "redirect:/chat/" + matchId;
	}
}

