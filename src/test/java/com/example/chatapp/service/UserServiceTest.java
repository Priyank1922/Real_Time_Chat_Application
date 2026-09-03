package com.example.chatapp.service;

import com.example.chatapp.dto.request.CreateUserRequest;
import com.example.chatapp.dto.request.UpdateUserRequest;
import com.example.chatapp.dto.response.RoomResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RoomMember;
import com.example.chatapp.entity.User;
import com.example.chatapp.enums.RoomMemberRole;
import com.example.chatapp.enums.RoomType;
import com.example.chatapp.enums.UserStatus;
import com.example.chatapp.exception.ConflictException;
import com.example.chatapp.exception.ResourceNotFoundException;
import com.example.chatapp.repository.RoomMemberRepository;
import com.example.chatapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("priyank")
                .email("priyank@example.com")
                .status(UserStatus.OFFLINE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("1. User creation - success")
    void testCreateUser_Success() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("priyank")
                .email("priyank@example.com")
                .build();

        when(userRepository.existsByUsername("priyank")).thenReturn(false);
        when(userRepository.existsByEmail("priyank@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("priyank", response.getUsername());
        assertEquals("priyank@example.com", response.getEmail());
        assertEquals(UserStatus.OFFLINE, response.getStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("2. User creation - duplicate username throws ConflictException")
    void testCreateUser_DuplicateUsername_ThrowsConflict() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("priyank")
                .email("new@example.com")
                .build();

        when(userRepository.existsByUsername("priyank")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("3. User creation - duplicate email throws ConflictException")
    void testCreateUser_DuplicateEmail_ThrowsConflict() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .email("priyank@example.com")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("priyank@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Get user by ID - found")
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Get user by ID - not found throws ResourceNotFoundException")
    void testGetUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    @DisplayName("Update user - success")
    void testUpdateUser_Success() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("priyank_updated")
                .email("updated@example.com")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByUsernameAndIdNot("priyank_updated", 1L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("updated@example.com", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = userService.updateUser(1L, request);

        assertNotNull(response);
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Get user rooms - returns active rooms")
    void testGetUserRooms_Success() {
        ChatRoom room = ChatRoom.builder()
                .id(10L)
                .name("Java Placement Preparation")
                .description("Discussion room")
                .roomType(RoomType.STUDY)
                .createdBy(sampleUser)
                .active(true)
                .build();

        RoomMember member = RoomMember.builder()
                .id(1L)
                .room(room)
                .user(sampleUser)
                .role(RoomMemberRole.OWNER)
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(roomMemberRepository.findByUserIdAndActiveTrue(1L)).thenReturn(List.of(member));
        when(roomMemberRepository.countByRoomIdAndActiveTrue(10L)).thenReturn(1L);

        List<RoomResponse> rooms = userService.getUserRooms(1L);

        assertEquals(1, rooms.size());
        assertEquals("Java Placement Preparation", rooms.get(0).getName());
        assertEquals(1L, rooms.get(0).getMemberCount());
    }
}
