package com.example.chatapp.websocket;

import com.example.chatapp.dto.request.SendMessageRequest;
import com.example.chatapp.dto.request.WebSocketMessageRequest;
import com.example.chatapp.dto.request.WebSocketTypingRequest;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.enums.MessagePriority;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.PresenceService;
import com.example.chatapp.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private PresenceService presenceService;

    @Mock
    private UserService userService;

    @InjectMocks
    private WebSocketController webSocketController;

    @Test
    @DisplayName("WebSocket /app/rooms/{roomId}/send forwards to MessageService")
    void testSendMessage() {
        WebSocketMessageRequest request = WebSocketMessageRequest.builder()
                .senderId(1L)
                .content("Hello WebSocket")
                .priority(MessagePriority.IMPORTANT)
                .build();

        webSocketController.sendMessage(10L, request);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(messageService).sendMessage(eq(10L), captor.capture());
        assertEquals(1L, captor.getValue().getSenderId());
        assertEquals("Hello WebSocket", captor.getValue().getContent());
        assertEquals(MessagePriority.IMPORTANT, captor.getValue().getPriority());
    }

    @Test
    @DisplayName("WebSocket /app/rooms/{roomId}/typing triggers presence typing")
    void testUserTyping() {
        WebSocketTypingRequest request = WebSocketTypingRequest.builder()
                .userId(15L)
                .username("Rahul")
                .build();

        webSocketController.userTyping(10L, request);

        verify(presenceService).handleTyping(10L, 15L, "Rahul");
    }

    @Test
    @DisplayName("WebSocket /app/rooms/{roomId}/stop-typing triggers presence stop typing")
    void testUserStoppedTyping() {
        WebSocketTypingRequest request = WebSocketTypingRequest.builder()
                .userId(15L)
                .username("Rahul")
                .build();

        webSocketController.userStoppedTyping(10L, request);

        verify(presenceService).handleStopTyping(10L, 15L, "Rahul");
    }

    @Test
    @DisplayName("WebSocket /app/presence/register registers session")
    void testRegisterPresence() {
        WebSocketTypingRequest request = WebSocketTypingRequest.builder()
                .userId(15L)
                .build();

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId("sess-xyz");

        webSocketController.registerPresence(request, headerAccessor);

        verify(presenceService).registerSession("sess-xyz", 15L);
    }
}
