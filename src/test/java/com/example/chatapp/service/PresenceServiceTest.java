package com.example.chatapp.service;

import com.example.chatapp.dto.response.WebSocketEventResponse;
import com.example.chatapp.entity.User;
import com.example.chatapp.enums.UserStatus;
import com.example.chatapp.enums.WebSocketEventType;
import com.example.chatapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private PresenceService presenceService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(15L)
                .username("Rahul")
                .email("rahul@example.com")
                .status(UserStatus.OFFLINE)
                .build();
    }

    @Test
    @DisplayName("15. User presence - first session connects marks user ONLINE and broadcasts USER_ONLINE")
    void testRegisterSession_FirstSession_MarksUserOnline() {
        when(userRepository.findById(15L)).thenReturn(Optional.of(sampleUser));

        presenceService.registerSession("sess-1", 15L);

        assertTrue(presenceService.isUserOnline(15L));
        assertEquals(1, presenceService.getActiveSessionCount(15L));
        assertEquals(UserStatus.ONLINE, sampleUser.getStatus());
        assertNotNull(sampleUser.getLastSeen());
        verify(userRepository).save(sampleUser);

        ArgumentCaptor<WebSocketEventResponse> captor = ArgumentCaptor.forClass(WebSocketEventResponse.class);
        verify(simpMessagingTemplate).convertAndSend(eq("/topic/presence"), captor.capture());
        assertEquals(WebSocketEventType.USER_ONLINE, captor.getValue().getEventType());
        assertEquals(15L, captor.getValue().getUserId());
        assertEquals("Rahul", captor.getValue().getUsername());
    }

    @Test
    @DisplayName("16 & 17. Multi-session presence - User remains ONLINE until all sessions disconnect")
    void testMultiSessionPresence_RemainsOnlineUntilLastDisconnect() {
        when(userRepository.findById(15L)).thenReturn(Optional.of(sampleUser));

        // Connect Session 1
        presenceService.registerSession("sess-1", 15L);
        assertTrue(presenceService.isUserOnline(15L));
        assertEquals(1, presenceService.getActiveSessionCount(15L));

        // Connect Session 2 (second tab / device)
        presenceService.registerSession("sess-2", 15L);
        assertTrue(presenceService.isUserOnline(15L));
        assertEquals(2, presenceService.getActiveSessionCount(15L));

        // Disconnect Session 1 -> User MUST remain ONLINE
        presenceService.removeSession("sess-1");
        assertTrue(presenceService.isUserOnline(15L));
        assertEquals(1, presenceService.getActiveSessionCount(15L));
        assertEquals(UserStatus.ONLINE, sampleUser.getStatus());

        // Disconnect Session 2 -> Last session gone, user transitions to OFFLINE
        presenceService.removeSession("sess-2");
        assertFalse(presenceService.isUserOnline(15L));
        assertEquals(0, presenceService.getActiveSessionCount(15L));
        assertEquals(UserStatus.OFFLINE, sampleUser.getStatus());

        ArgumentCaptor<WebSocketEventResponse> offlineCaptor = ArgumentCaptor.forClass(WebSocketEventResponse.class);
        verify(simpMessagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/presence"), offlineCaptor.capture());
        assertEquals(WebSocketEventType.USER_OFFLINE, offlineCaptor.getValue().getEventType());
        assertEquals(15L, offlineCaptor.getValue().getUserId());
    }

    @Test
    @DisplayName("18. Typing event - broadcasts USER_TYPING to room events topic")
    void testHandleTyping() {
        presenceService.handleTyping(10L, 15L, "Rahul");

        ArgumentCaptor<WebSocketEventResponse> captor = ArgumentCaptor.forClass(WebSocketEventResponse.class);
        verify(simpMessagingTemplate).convertAndSend(eq("/topic/rooms/10/events"), captor.capture());
        assertEquals(WebSocketEventType.USER_TYPING, captor.getValue().getEventType());
        assertEquals(10L, captor.getValue().getRoomId());
        assertEquals(15L, captor.getValue().getUserId());
        assertEquals("Rahul", captor.getValue().getUsername());
    }

    @Test
    @DisplayName("18b. Stop typing event - broadcasts USER_STOPPED_TYPING to room events topic")
    void testHandleStopTyping() {
        presenceService.handleStopTyping(10L, 15L, "Rahul");

        ArgumentCaptor<WebSocketEventResponse> captor = ArgumentCaptor.forClass(WebSocketEventResponse.class);
        verify(simpMessagingTemplate).convertAndSend(eq("/topic/rooms/10/events"), captor.capture());
        assertEquals(WebSocketEventType.USER_STOPPED_TYPING, captor.getValue().getEventType());
        assertEquals(10L, captor.getValue().getRoomId());
        assertEquals(15L, captor.getValue().getUserId());
    }

    @Test
    @DisplayName("Thread safety - concurrent sessions registration and removal")
    void testConcurrentSessionHandling() throws InterruptedException {
        when(userRepository.findById(any())).thenReturn(Optional.of(sampleUser));

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String sessId = "session-" + i;
            executor.submit(() -> {
                try {
                    presenceService.registerSession(sessId, 15L);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(threadCount, presenceService.getActiveSessionCount(15L));
        assertTrue(presenceService.isUserOnline(15L));

        CountDownLatch removeLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final String sessId = "session-" + i;
            executor.submit(() -> {
                try {
                    presenceService.removeSession(sessId);
                } finally {
                    removeLatch.countDown();
                }
            });
        }

        assertTrue(removeLatch.await(5, TimeUnit.SECONDS));
        assertEquals(0, presenceService.getActiveSessionCount(15L));
        assertFalse(presenceService.isUserOnline(15L));
        executor.shutdown();
    }
}
