package com.tibay.tibayai.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tibay.tibayai.entity.Match;
import com.tibay.tibayai.entity.Message;
import com.tibay.tibayai.entity.NotificationType;
import com.tibay.tibayai.entity.User;
import com.tibay.tibayai.repo.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {
	private final MessageRepository messageRepository;
	private final NotificationService notificationService;

	@Transactional
	public Message send(Match match, User sender, String body) {
		Message m = new Message();
		m.setMatch(match);
		m.setSenderUser(sender);
		m.setBody(body);
		messageRepository.save(m);

		User recipient = match.getWorkerProfile().getUser().getId().equals(sender.getId())
				? match.getClientProfile().getUser()
				: match.getWorkerProfile().getUser();

		notificationService.notify(recipient, NotificationType.MESSAGE, "New message about " + match.getJobPost().getTitle(),
				"/chat/" + match.getId());
		return m;
	}
}

