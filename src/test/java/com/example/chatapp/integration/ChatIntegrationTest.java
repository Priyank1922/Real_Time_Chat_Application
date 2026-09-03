package com.example.chatapp.integration;

import com.example.chatapp.config.CacheConfig;
import com.example.chatapp.dto.request.CreateRoomRequest;
import com.example.chatapp.dto.request.CreateUserRequest;
import com.example.chatapp.dto.request.SendMessageRequest;
import com.example.chatapp.dto.request.UpdateMessageRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.response.RoomMemberResponse;
import com.example.chatapp.dto.response.RoomResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.enums.MessagePriority;
import com.example.chatapp.enums.RoomType;
import com.example.chatapp.enums.UserStatus;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.PresenceService;
import com.example.chatapp.service.RoomService;
import com.example.chatapp.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChatIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("End-to-End Chat Flow: User creation, Room creation, Joining, Messaging, and Soft Delete")
    void testCompleteChatFlow() {
        // 1. Create User 1 (Owner) and User 2 (Member)
        UserResponse user1 = userService.createUser(CreateUserRequest.builder()
                .username("priyank")
                .email("priyank@example.com")
                .build());
        assertNotNull(user1.getId());

        UserResponse user2 = userService.createUser(CreateUserRequest.builder()
                .username("rahul")
                .email("rahul@example.com")
                .build());
        assertNotNull(user2.getId());

        // 2. Create Context-based Room
        RoomResponse room = roomService.createRoom(CreateRoomRequest.builder()
                .name("Java Placement Preparation")
                .description("Discussion room for Java and Spring Boot")
                .roomType(RoomType.STUDY)
                .userId(user1.getId())
                .build());
        assertEquals(1L, room.getMemberCount());
        assertEquals("Java Placement Preparation", room.getName());

        // 3. User 2 Joins the Room
        RoomMemberResponse member2 = roomService.joinRoom(room.getId(), user2.getId());
        assertEquals(user2.getId(), member2.getUserId());

        // 4. Presence Tracking: User 2 connects 2 tabs
        presenceService.registerSession("session-tab-1", user2.getId());
        presenceService.registerSession("session-tab-2", user2.getId());
        assertTrue(presenceService.isUserOnline(user2.getId()));
        assertEquals(2, presenceService.getActiveSessionCount(user2.getId()));

        // Tab 1 closes -> User 2 still ONLINE
        presenceService.removeSession("session-tab-1");
        assertTrue(presenceService.isUserOnline(user2.getId()));
        assertEquals(1, presenceService.getActiveSessionCount(user2.getId()));

        // 5. Send Normal and Urgent Messages
        MessageResponse msg1 = messageService.sendMessage(room.getId(), SendMessageRequest.builder()
                .senderId(user1.getId())
                .content("Anyone solving today's DSA problem?")
                .priority(MessagePriority.NORMAL)
                .build());
        assertEquals(MessagePriority.NORMAL, msg1.getPriority());

        MessageResponse msg2 = messageService.sendMessage(room.getId(), SendMessageRequest.builder()
                .senderId(user2.getId())
                .content("Production server is down!")
                .priority(MessagePriority.URGENT)
                .build());
        assertEquals(MessagePriority.URGENT, msg2.getPriority());

        // 6. Paginated Message Retrieval
        Page<MessageResponse> page = messageService.getRoomMessages(room.getId(), PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());

        // 7. Update Message by Sender
        MessageResponse updatedMsg = messageService.updateMessage(msg1.getId(), UpdateMessageRequest.builder()
                .userId(user1.getId())
                .content("Anyone solving today's LeetCode daily problem?")
                .build());
        assertTrue(updatedMsg.isEdited());
        assertEquals("Anyone solving today's LeetCode daily problem?", updatedMsg.getContent());

        // 8. Soft Delete Message
        messageService.deleteMessage(msg2.getId(), user2.getId());
        MessageResponse deletedMsg = messageService.getMessageById(msg2.getId());
        assertTrue(deletedMsg.isDeleted());
        assertEquals(MessageService.DELETED_MESSAGE_PLACEHOLDER, deletedMsg.getContent());

        // 9. Tab 2 closes -> User 2 now OFFLINE
        presenceService.removeSession("session-tab-2");
        assertFalse(presenceService.isUserOnline(user2.getId()));
    }

    @Test
    @DisplayName("19. Spring Cache behavior - verifying caching and cache eviction on mutation")
    void testCacheBehavior() {
        UserResponse user = userService.createUser(CreateUserRequest.builder()
                .username("cacheUser")
                .email("cache@example.com")
                .build());

        RoomResponse room = roomService.createRoom(CreateRoomRequest.builder()
                .name("Cached Room")
                .description("Testing cache")
                .roomType(RoomType.PROJECT)
                .userId(user.getId())
                .build());

        // First call caches room details
        RoomResponse fetched1 = roomService.getRoomById(room.getId());
        assertNotNull(fetched1);

        Cache detailsCache = cacheManager.getCache(CacheConfig.ROOM_DETAILS_CACHE);
        assertNotNull(detailsCache);
        assertNotNull(detailsCache.get(room.getId()));

        // Also test rooms list cache
        List<RoomResponse> rooms = roomService.getRooms(null, null);
        assertFalse(rooms.isEmpty());

        Cache roomsCache = cacheManager.getCache(CacheConfig.ROOMS_CACHE);
        assertNotNull(roomsCache);

        // Deleting room should evict caches
        roomService.deleteRoom(room.getId(), user.getId());
        assertNull(detailsCache.get(room.getId()));
    }
}
