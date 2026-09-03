package com.example.chatapp.websocket;

import com.example.chatapp.dto.request.SendMessageRequest;
import com.example.chatapp.dto.request.WebSocketMessageRequest;
import com.example.chatapp.dto.request.WebSocketTypingRequest;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.PresenceService;
import com.example.chatapp.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final MessageService messageService;
    private final PresenceService presenceService;
    private final UserService userService;

    public WebSocketController(MessageService messageService, PresenceService presenceService, UserService userService) {
        this.messageService = messageService;
        this.presenceService = presenceService;
        this.userService = userService;
    }

    /**
     * Handles sending messages to a chat room over STOMP.
     * Client sends to: /app/rooms/{roomId}/send
     */
    @MessageMapping("/rooms/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload @Valid WebSocketMessageRequest request) {
        log.info("Received WebSocket message for room {}: senderId={}, priority={}",
                roomId, request.getSenderId(), request.getPriority());

        SendMessageRequest sendRequest = SendMessageRequest.builder()
                .senderId(request.getSenderId())
                .content(request.getContent())
                .priority(request.getPriority())
                .build();

        messageService.sendMessage(roomId, sendRequest);
    }

    /**
     * Handles typing indicator event.
     * Client sends to: /app/rooms/{roomId}/typing
     */
    @MessageMapping("/rooms/{roomId}/typing")
    public void userTyping(
            @DestinationVariable Long roomId,
            @Payload @Valid WebSocketTypingRequest request) {
        String username = request.getUsername();
        if (username == null || username.isBlank()) {
            username = userService.getUserById(request.getUserId()).getUsername();
        }
        presenceService.handleTyping(roomId, request.getUserId(), username);
    }

    /**
     * Handles stopped typing indicator event.
     * Client sends to: /app/rooms/{roomId}/stop-typing
     */
    @MessageMapping("/rooms/{roomId}/stop-typing")
    public void userStoppedTyping(
            @DestinationVariable Long roomId,
            @Payload @Valid WebSocketTypingRequest request) {
        String username = request.getUsername();
        if (username == null || username.isBlank()) {
            username = userService.getUserById(request.getUserId()).getUsername();
        }
        presenceService.handleStopTyping(roomId, request.getUserId(), username);
    }

    /**
     * Explicit session registration from client.
     * Client sends to: /app/presence/register
     */
    @MessageMapping("/presence/register")
    public void registerPresence(
            @Payload @Valid WebSocketTypingRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Explicit presence registration for userId {} with sessionId {}", request.getUserId(), sessionId);
        presenceService.registerSession(sessionId, request.getUserId());
    }
}
