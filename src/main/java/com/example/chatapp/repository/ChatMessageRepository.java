package com.example.chatapp.repository;

import com.example.chatapp.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    Page<ChatMessage> findByRoomIdAndDeletedFalseOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    Optional<ChatMessage> findByIdAndRoomId(Long id, Long roomId);
}
