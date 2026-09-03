package com.example.chatapp.service;

import com.example.chatapp.dto.response.WebSocketEventResponse;
import com.example.chatapp.entity.User;
import com.example.chatapp.enums.UserStatus;
import com.example.chatapp.enums.WebSocketEventType;
import com.example.chatapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private final UserRepository userRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    // Thread-safe in-memory session registries
    private final ConcurrentHashMap<String, Long> sessionToUserMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<String>> userToSessionsMap = new ConcurrentHashMap<>();

    public PresenceService(UserRepository userRepository, SimpMessagingTemplate simpMessagingTemplate) {
        this.userRepository = userRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Transactional
    public void registerSession(String sessionId, Long userId) {
        if (sessionId == null || userId == null) {
            return;
        }

        sessionToUserMap.put(sessionId, userId);
        Set<String> sessions = userToSessionsMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        boolean wasOffline = sessions.isEmpty();
        sessions.add(sessionId);

        log.info("User {} connected session {}. Total active sessions: {}", userId, sessionId, sessions.size());

        if (wasOffline) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                LocalDateTime now = LocalDateTime.now();
                user.setStatus(UserStatus.ONLINE);
                user.setLastSeen(now);
                userRepository.save(user);

                WebSocketEventResponse onlineEvent = WebSocketEventResponse.builder()
                        .eventType(WebSocketEventType.USER_ONLINE)
                        .userId(user.getId())
                        .username(user.getUsername())
                        .timestamp(now)
                        .build();

                simpMessagingTemplate.convertAndSend("/topic/presence", onlineEvent);
                log.info("Broadcasted USER_ONLINE for user {} ({})", user.getId(), user.getUsername());
            }
        }
    }

    @Transactional
    public void removeSession(String sessionId) {
        if (sessionId == null) {
            return;
        }

        Long userId = sessionToUserMap.remove(sessionId);
        if (userId == null) {
            return;
        }

        Set<String> sessions = userToSessionsMap.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            log.info("Removed session {} for user {}. Remaining active sessions: {}", sessionId, userId, sessions.size());

            if (sessions.isEmpty()) {
                userToSessionsMap.remove(userId, Collections.emptySet());
                if (!userToSessionsMap.containsKey(userId) || userToSessionsMap.get(userId).isEmpty()) {
                    userToSessionsMap.remove(userId);

                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        LocalDateTime now = LocalDateTime.now();
                        user.setStatus(UserStatus.OFFLINE);
                        user.setLastSeen(now);
                        userRepository.save(user);

                        WebSocketEventResponse offlineEvent = WebSocketEventResponse.builder()
                                .eventType(WebSocketEventType.USER_OFFLINE)
                                .userId(user.getId())
                                .username(user.getUsername())
                                .lastSeen(now)
                                .timestamp(now)
                                .build();

                        simpMessagingTemplate.convertAndSend("/topic/presence", offlineEvent);
                        log.info("Broadcasted USER_OFFLINE for user {} ({})", user.getId(), user.getUsername());
                    }
                }
            }
        }
    }

    public boolean isUserOnline(Long userId) {
        Set<String> sessions = userToSessionsMap.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public int getActiveSessionCount(Long userId) {
        Set<String> sessions = userToSessionsMap.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    public void handleTyping(Long roomId, Long userId, String username) {
        WebSocketEventResponse typingEvent = WebSocketEventResponse.builder()
                .eventType(WebSocketEventType.USER_TYPING)
                .roomId(roomId)
                .userId(userId)
                .username(username)
                .timestamp(LocalDateTime.now())
                .build();

        simpMessagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", typingEvent);
    }

    public void handleStopTyping(Long roomId, Long userId, String username) {
        WebSocketEventResponse stopTypingEvent = WebSocketEventResponse.builder()
                .eventType(WebSocketEventType.USER_STOPPED_TYPING)
                .roomId(roomId)
                .userId(userId)
                .username(username)
                .timestamp(LocalDateTime.now())
                .build();

        simpMessagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", stopTypingEvent);
    }
}
