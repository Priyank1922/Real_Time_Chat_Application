package com.example.chatapp.service;

import com.example.chatapp.dto.request.SendMessageRequest;
import com.example.chatapp.dto.request.UpdateMessageRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.response.WebSocketEventResponse;
import com.example.chatapp.entity.ChatMessage;
import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RoomMember;
import com.example.chatapp.entity.User;
import com.example.chatapp.enums.MessagePriority;
import com.example.chatapp.enums.RoomMemberRole;
import com.example.chatapp.enums.RoomType;
import com.example.chatapp.enums.WebSocketEventType;
import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.exception.UnauthorizedActionException;
import com.example.chatapp.repository.ChatMessageRepository;
import com.example.chatapp.repository.RoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserService userService;

    @Mock
    private RoomService roomService;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private User otherUser;
    private ChatRoom sampleRoom;
    private ChatMessage sampleMessage;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L)
                .username("priyank")
                .email("priyank@example.com")
                .build();

        otherUser = User.builder()
                .id(2L)
                .username("other")
                .email("other@example.com")
                .build();

        sampleRoom = ChatRoom.builder()
                .id(10L)
                .name("Java Placement Preparation")
                .roomType(RoomType.STUDY)
                .createdBy(sender)
                .active(true)
                .build();

        sampleMessage = ChatMessage.builder()
                .id(101L)
                .room(sampleRoom)
                .sender(sender)
                .content("Anyone solving today's DSA problem?")
                .priority(MessagePriority.NORMAL)
                .edited(false)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("9. Message creation - success")
    void testSendMessage_Success() {
        SendMessageRequest request = SendMessageRequest.builder()
                .senderId(1L)
                .content("Anyone solving today's DSA problem?")
                .priority(MessagePriority.NORMAL)
                .build();

        when(roomService.getRoomEntityById(10L)).thenReturn(sampleRoom);
        when(userService.getUserEntityById(1L)).thenReturn(sender);
        when(roomMemberRepository.existsByRoomIdAndUserIdAndActiveTrue(10L, 1L)).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(sampleMessage);

        MessageResponse response = messageService.sendMessage(10L, request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("Anyone solving today's DSA problem?", response.getContent());
        assertEquals(MessagePriority.NORMAL, response.getPriority());

        // Verify WebSocket broadcast
        verify(simpMessagingTemplate).convertAndSend(eq("/topic/rooms/10/messages"), any(WebSocketEventResponse.class));
    }

    @Test
    @DisplayName("10. Message priority - URGENT message broadcasts URGENT_MESSAGE event")
    void testSendMessage_UrgentPriority_BroadcastsUrgentEvent() {
        SendMessageRequest request = SendMessageRequest.builder()
                .senderId(1L)
                .content("Production server is down!")
                .priority(MessagePriority.URGENT)
                .build();

        ChatMessage urgentMessage = ChatMessage.builder()
                .id(102L)
                .room(sampleRoom)
                .sender(sender)
                .content("Production server is down!")
                .priority(MessagePriority.URGENT)
                .edited(false)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(roomService.getRoomEntityById(10L)).thenReturn(sampleRoom);
        when(userService.getUserEntityById(1L)).thenReturn(sender);
        when(roomMemberRepository.existsByRoomIdAndUserIdAndActiveTrue(10L, 1L)).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(urgentMessage);

        MessageResponse response = messageService.sendMessage(10L, request);

        assertEquals(MessagePriority.URGENT, response.getPriority());

        ArgumentCaptor<WebSocketEventResponse> captor = ArgumentCaptor.forClass(WebSocketEventResponse.class);
        verify(simpMessagingTemplate).convertAndSend(eq("/topic/rooms/10/messages"), captor.capture());
        assertEquals(WebSocketEventType.URGENT_MESSAGE, captor.getValue().getEventType());
        assertEquals(MessagePriority.URGENT, captor.getValue().getPriority());
    }

    @Test
    @DisplayName("11. Message validation - user not member throws UnauthorizedActionException")
    void testSendMessage_NonMember_ThrowsUnauthorized() {
        SendMessageRequest request = SendMessageRequest.builder()
                .senderId(2L)
                .content("Hello")
                .priority(MessagePriority.NORMAL)
                .build();

        when(roomService.getRoomEntityById(10L)).thenReturn(sampleRoom);
        when(userService.getUserEntityById(2L)).thenReturn(otherUser);
        when(roomMemberRepository.existsByRoomIdAndUserIdAndActiveTrue(10L, 2L)).thenReturn(false);

        assertThrows(UnauthorizedActionException.class, () -> messageService.sendMessage(10L, request));
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("11b. Message validation - empty content throws BadRequestException")
    void testSendMessage_EmptyContent_ThrowsBadRequest() {
        SendMessageRequest request = SendMessageRequest.builder()
                .senderId(1L)
                .content("   ")
                .priority(MessagePriority.NORMAL)
                .build();

        when(roomService.getRoomEntityById(10L)).thenReturn(sampleRoom);
        when(userService.getUserEntityById(1L)).thenReturn(sender);
        when(roomMemberRepository.existsByRoomIdAndUserIdAndActiveTrue(10L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> messageService.sendMessage(10L, request));
    }

    @Test
    @DisplayName("12. Message history pagination - returns paginated responses")
    void testGetRoomMessages_Pagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ChatMessage> messagePage = new PageImpl<>(List.of(sampleMessage), pageable, 1);

        when(roomService.getRoomEntityById(10L)).thenReturn(sampleRoom);
        when(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(10L, pageable)).thenReturn(messagePage);

        Page<MessageResponse> responses = messageService.getRoomMessages(10L, pageable);

        assertNotNull(responses);
        assertEquals(1, responses.getContent().size());
        assertEquals("Anyone solving today's DSA problem?", responses.getContent().get(0).getContent());
    }

    @Test
    @DisplayName("13. Message update - sender edits message, sets edited=true and broadcasts event")
    void testUpdateMessage_Success() {
        UpdateMessageRequest request = UpdateMessageRequest.builder()
                .userId(1L)
                .content("Updated content")
                .build();

        when(chatMessageRepository.findById(101L)).thenReturn(Optional.of(sampleMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(sampleMessage);

        MessageResponse response = messageService.updateMessage(101L, request);

        assertTrue(sampleMessage.isEdited());
        assertEquals("Updated content", sampleMessage.getContent());
        verify(simpMessagingTemplate).convertAndSend(
                eq("/topic/rooms/10/messages"),
                argThat((WebSocketEventResponse e) -> e.getEventType() == WebSocketEventType.MESSAGE_UPDATED)
        );
    }

    @Test
    @DisplayName("13b. Message update - non-sender editing throws UnauthorizedActionException")
    void testUpdateMessage_ByNonSender_ThrowsUnauthorized() {
        UpdateMessageRequest request = UpdateMessageRequest.builder()
                .userId(2L)
                .content("Hacked content")
                .build();

        when(chatMessageRepository.findById(101L)).thenReturn(Optional.of(sampleMessage));

        assertThrows(UnauthorizedActionException.class, () -> messageService.updateMessage(101L, request));
    }

    @Test
    @DisplayName("14. Message deletion - soft delete masks content and broadcasts event")
    void testDeleteMessage_SoftDelete_Success() {
        when(chatMessageRepository.findById(101L)).thenReturn(Optional.of(sampleMessage));
        when(userService.getUserEntityById(1L)).thenReturn(sender);

        messageService.deleteMessage(101L, 1L);

        assertTrue(sampleMessage.isDeleted());
        verify(chatMessageRepository).save(sampleMessage);
        verify(simpMessagingTemplate).convertAndSend(
                eq("/topic/rooms/10/messages"),
                argThat((WebSocketEventResponse e) -> e.getEventType() == WebSocketEventType.MESSAGE_DELETED)
        );

        // Verify mapToMessageResponse sanitizes deleted message content
        MessageResponse response = messageService.mapToMessageResponse(sampleMessage);
        assertEquals(MessageService.DELETED_MESSAGE_PLACEHOLDER, response.getContent());
        assertTrue(response.isDeleted());
    }
}
