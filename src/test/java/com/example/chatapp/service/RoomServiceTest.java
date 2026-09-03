package com.example.chatapp.service;

import com.example.chatapp.dto.request.CreateRoomRequest;
import com.example.chatapp.dto.request.UpdateRoomRequest;
import com.example.chatapp.dto.response.RoomMemberResponse;
import com.example.chatapp.dto.response.RoomResponse;
import com.example.chatapp.dto.response.WebSocketEventResponse;
import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RoomMember;
import com.example.chatapp.entity.User;
import com.example.chatapp.enums.RoomMemberRole;
import com.example.chatapp.enums.RoomType;
import com.example.chatapp.enums.WebSocketEventType;
import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.exception.ConflictException;
import com.example.chatapp.exception.UnauthorizedActionException;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.RoomMemberRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserService userService;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private RoomService roomService;

    private User owner;
    private User regularUser;
    private ChatRoom sampleRoom;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .username("ownerUser")
                .email("owner@example.com")
                .build();

        regularUser = User.builder()
                .id(2L)
                .username("regularUser")
                .email("regular@example.com")
                .build();

        sampleRoom = ChatRoom.builder()
                .id(10L)
                .name("Java Placement Preparation")
                .description("Discussion room")
                .roomType(RoomType.STUDY)
                .createdBy(owner)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("4. Room creation - sets owner and saves member")
    void testCreateRoom_Success() {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .name("Java Placement Preparation")
                .description("Discussion room")
                .roomType(RoomType.STUDY)
                .userId(1L)
                .build();

        when(userService.getUserEntityById(1L)).thenReturn(owner);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(sampleRoom);

        RoomResponse response = roomService.createRoom(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Java Placement Preparation", response.getName());
        assertEquals(1L, response.getCreatedById());
        assertEquals(1L, response.getMemberCount());

        ArgumentCaptor<RoomMember> memberCaptor = ArgumentCaptor.forClass(RoomMember.class);
        verify(roomMemberRepository).save(memberCaptor.capture());
        RoomMember createdMember = memberCaptor.getValue();
        assertEquals(RoomMemberRole.OWNER, createdMember.getRole());
        assertTrue(createdMember.isActive());
    }

    @Test
    @DisplayName("5. Room joining - user successfully joins and event broadcasted")
    void testJoinRoom_Success() {
        when(chatRoomRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(sampleRoom));
        when(userService.getUserEntityById(2L)).thenReturn(regularUser);
        when(roomMemberRepository.findByRoomIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        RoomMember savedMember = RoomMember.builder()
                .id(20L)
                .room(sampleRoom)
                .user(regularUser)
                .role(RoomMemberRole.MEMBER)
                .active(true)
                .joinedAt(LocalDateTime.now())
                .build();
        when(roomMemberRepository.save(any(RoomMember.class))).thenReturn(savedMember);

        RoomMemberResponse response = roomService.joinRoom(10L, 2L);

        assertNotNull(response);
        assertEquals(2L, response.getUserId());
        assertEquals(10L, response.getRoomId());
        assertEquals(RoomMemberRole.MEMBER, response.getRole());

        // Verify WebSocket broadcast
        verify(simpMessagingTemplate).convertAndSend(
                eq("/topic/rooms/10/events"),
                any(WebSocketEventResponse.class)
        );
    }

    @Test
    @DisplayName("6. Duplicate room joining - throws ConflictException")
    void testJoinRoom_DuplicateJoin_ThrowsConflict() {
        when(chatRoomRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(sampleRoom));
        when(userService.getUserEntityById(2L)).thenReturn(regularUser);

        RoomMember existingActiveMember = RoomMember.builder()
                .id(20L)
                .room(sampleRoom)
                .user(regularUser)
                .role(RoomMemberRole.MEMBER)
                .active(true)
                .build();
        when(roomMemberRepository.findByRoomIdAndUserId(10L, 2L)).thenReturn(Optional.of(existingActiveMember));

        assertThrows(ConflictException.class, () -> roomService.joinRoom(10L, 2L));
        verify(roomMemberRepository, never()).save(any(RoomMember.class));
    }

    @Test
    @DisplayName("7. Room leaving - marks member inactive and broadcasts event")
    void testLeaveRoom_Success() {
        when(chatRoomRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(sampleRoom));
        when(userService.getUserEntityById(2L)).thenReturn(regularUser);

        RoomMember activeMember = RoomMember.builder()
                .id(20L)
                .room(sampleRoom)
                .user(regularUser)
                .role(RoomMemberRole.MEMBER)
                .active(true)
                .build();
        when(roomMemberRepository.findByRoomIdAndUserIdAndActiveTrue(10L, 2L)).thenReturn(Optional.of(activeMember));

        roomService.leaveRoom(10L, 2L);

        assertFalse(activeMember.isActive());
        verify(roomMemberRepository).save(activeMember);
        verify(simpMessagingTemplate).convertAndSend(
                eq("/topic/rooms/10/events"),
                argThat((WebSocketEventResponse event) -> event.getEventType() == WebSocketEventType.ROOM_USER_LEFT)
        );
    }

    @Test
    @DisplayName("Leave room - user not a member throws BadRequestException")
    void testLeaveRoom_NotMember_ThrowsBadRequest() {
        when(chatRoomRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(sampleRoom));
        when(userService.getUserEntityById(2L)).thenReturn(regularUser);
        when(roomMemberRepository.findByRoomIdAndUserIdAndActiveTrue(10L, 2L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> roomService.leaveRoom(10L, 2L));
    }

    @Test
    @DisplayName("8. Unauthorized business operation - delete room by non-owner throws UnauthorizedActionException")
    void testDeleteRoom_ByNonOwner_ThrowsUnauthorized() {
        when(chatRoomRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(sampleRoom));
        when(userService.getUserEntityById(2L)).thenReturn(regularUser);
        when(roomMemberRepository.findByRoomIdAndUserIdAndActiveTrue(10L, 2L)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedActionException.class, () -> roomService.deleteRoom(10L, 2L));
        assertTrue(sampleRoom.isActive());
    }

    @Test
    @DisplayName("Delete room - by owner succeeds (soft delete)")
    void testDeleteRoom_ByOwner_Success() {
        when(chatRoomRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(sampleRoom));
        when(userService.getUserEntityById(1L)).thenReturn(owner);

        roomService.deleteRoom(10L, 1L);

        assertFalse(sampleRoom.isActive());
        verify(chatRoomRepository).save(sampleRoom);
    }

    @Test
    @DisplayName("Search rooms - returns all rooms when search and roomType are null")
    void testGetRooms_AllRooms() {
        when(chatRoomRepository.findByActiveTrue()).thenReturn(List.of(sampleRoom));
        when(roomMemberRepository.countByRoomIdAndActiveTrue(10L)).thenReturn(3L);

        List<RoomResponse> rooms = roomService.getRooms(null, null);

        assertEquals(1, rooms.size());
        assertEquals(3L, rooms.get(0).getMemberCount());
        verify(chatRoomRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("Search rooms - filter by room type only")
    void testGetRooms_FilterByRoomType() {
        when(chatRoomRepository.findByRoomTypeAndActiveTrue(RoomType.STUDY)).thenReturn(List.of(sampleRoom));
        when(roomMemberRepository.countByRoomIdAndActiveTrue(10L)).thenReturn(3L);

        List<RoomResponse> rooms = roomService.getRooms(null, RoomType.STUDY);

        assertEquals(1, rooms.size());
        assertEquals(3L, rooms.get(0).getMemberCount());
        verify(chatRoomRepository).findByRoomTypeAndActiveTrue(RoomType.STUDY);
    }

    @Test
    @DisplayName("Search rooms - filter by search term only")
    void testGetRooms_FilterBySearchTerm() {
        when(chatRoomRepository.searchByTerm("Java")).thenReturn(List.of(sampleRoom));
        when(roomMemberRepository.countByRoomIdAndActiveTrue(10L)).thenReturn(3L);

        List<RoomResponse> rooms = roomService.getRooms("Java", null);

        assertEquals(1, rooms.size());
        assertEquals(3L, rooms.get(0).getMemberCount());
        verify(chatRoomRepository).searchByTerm("Java");
    }

    @Test
    @DisplayName("Search rooms - filter by search term and room type")
    void testGetRooms_FilterBySearchAndRoomType() {
        when(chatRoomRepository.searchByTermAndRoomType("Java", RoomType.STUDY)).thenReturn(List.of(sampleRoom));
        when(roomMemberRepository.countByRoomIdAndActiveTrue(10L)).thenReturn(3L);

        List<RoomResponse> rooms = roomService.getRooms("Java", RoomType.STUDY);

        assertEquals(1, rooms.size());
        assertEquals(3L, rooms.get(0).getMemberCount());
        verify(chatRoomRepository).searchByTermAndRoomType("Java", RoomType.STUDY);
    }
}
