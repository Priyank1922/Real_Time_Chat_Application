package com.example.chatapp.websocket;

import com.example.chatapp.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PresenceService presenceService;

    public WebSocketEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleSessionConnectEvent(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        List<String> userIdHeader = accessor.getNativeHeader("userId");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdHeader.get(0));
                log.info("Client connected with session {} and userId header: {}", sessionId, userId);
                presenceService.registerSession(sessionId, userId);
            } catch (NumberFormatException e) {
                log.warn("Invalid userId header in STOMP connect: {}", userIdHeader.get(0));
            }
        }
    }

    @EventListener
    public void handleSessionConnectedEvent(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket connection established. Session: {}", accessor.getSessionId());
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("WebSocket session disconnected: {}", sessionId);
        presenceService.removeSession(sessionId);
    }
}
