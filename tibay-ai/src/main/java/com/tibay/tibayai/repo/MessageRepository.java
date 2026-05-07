package com.tibay.tibayai.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tibay.tibayai.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
	List<Message> findByMatchIdOrderBySentAtAsc(Long matchId);
}

