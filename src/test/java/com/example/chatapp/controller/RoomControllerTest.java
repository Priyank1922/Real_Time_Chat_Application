package com.example.chatapp.controller;

import com.example.chatapp.dto.request.CreateRoomRequest;
import com.example.chatapp.dto.request.SendMessageRequest;
import com.example.chatapp.dto.request.UpdateRoomRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.response.RoomMemberResponse;
import com.example.chatapp.dto.response.RoomResponse;
import com.example.chatapp.enums.MessagePriority;
import com.example.chatapp.enums.RoomMemberRole;
import com.example.chatapp.enums.RoomType;
import com.example.chatapp.exception.UnauthorizedActionException;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @MockBean
    private MessageService messageService;

    @Test
    @DisplayName("POST /api/rooms - returns 201 Created")
    void testCreateRoom_ReturnsCreated() throws Exception {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .name("Java Placement Preparation")
                .description("Discussion room")
                .roomType(RoomType.STUDY)
                .userId(1L)
                .build();

        RoomResponse response = RoomResponse.builder()
                .id(10L)
                .name("Java Placement Preparation")
                .description("Discussion room")
                .roomType(RoomType.STUDY)
                .createdById(1L)
                .createdByName("priyank")
                .memberCount(1L)
                .active(true)
                .build();

        when(roomService.createRoom(any(CreateRoomRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Java Placement Preparation"))
                .andExpect(jsonPath("$.createdById").value(1L));
    }

    @Test
    @DisplayName("GET /api/rooms - returns 200 OK")
    void testGetRooms_Success() throws Exception {
        RoomResponse response = RoomResponse.builder()
                .id(10L)
                .name("Java Placement Preparation")
                .roomType(RoomType.STUDY)
                .memberCount(2L)
                .active(true)
                .build();

        when(roomService.getRooms(null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].name").value("Java Placement Preparation"));
    }

    @Test
    @DisplayName("POST /api/rooms/{roomId}/join/{userId} - returns 201 Created")
    void testJoinRoom_ReturnsCreated() throws Exception {
        RoomMemberResponse response = RoomMemberResponse.builder()
                .id(100L)
                .userId(2L)
                .username("Rahul")
                .roomId(10L)
                .role(RoomMemberRole.MEMBER)
                .active(true)
                .joinedAt(LocalDateTime.now())
                .build();

        when(roomService.joinRoom(10L, 2L)).thenReturn(response);

        mockMvc.perform(post("/api/rooms/10/join/2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2L))
                .andExpect(jsonPath("$.roomId").value(10L))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    @DisplayName("DELETE /api/rooms/{roomId}/leave/{userId} - returns 204 No Content")
    void testLeaveRoom_ReturnsNoContent() throws Exception {
        doNothing().when(roomService).leaveRoom(10L, 2L);

        mockMvc.perform(delete("/api/rooms/10/leave/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/rooms/{roomId} by non-owner - returns 403 Forbidden")
    void testDeleteRoom_Unauthorized_Returns403() throws Exception {
        doThrow(new UnauthorizedActionException("Only the room owner can perform this operation"))
                .when(roomService).deleteRoom(10L, 2L);

        mockMvc.perform(delete("/api/rooms/10").param("userId", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN_ACTION"));
    }

    @Test
    @DisplayName("POST /api/rooms/{roomId}/messages - returns 201 Created")
    void testSendMessage_ReturnsCreated() throws Exception {
        SendMessageRequest request = SendMessageRequest.builder()
                .senderId(1L)
                .content("Anyone solving today's DSA problem?")
                .priority(MessagePriority.NORMAL)
                .build();

        MessageResponse response = MessageResponse.builder()
                .id(101L)
                .roomId(10L)
                .senderId(1L)
                .senderName("priyank")
                .content("Anyone solving today's DSA problem?")
                .priority(MessagePriority.NORMAL)
                .build();

        when(messageService.sendMessage(eq(10L), any(SendMessageRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rooms/10/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.content").value("Anyone solving today's DSA problem?"));
    }

    @Test
    @DisplayName("GET /api/rooms/{roomId}/messages - returns 200 OK paginated")
    void testGetRoomMessages_ReturnsPage() throws Exception {
        MessageResponse response = MessageResponse.builder()
                .id(101L)
                .roomId(10L)
                .senderId(1L)
                .senderName("priyank")
                .content("Anyone solving today's DSA problem?")
                .priority(MessagePriority.NORMAL)
                .build();

        Page<MessageResponse> page = new PageImpl<>(List.of(response));
        when(messageService.getRoomMessages(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/rooms/10/messages?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(101L))
                .andExpect(jsonPath("$.content[0].senderName").value("priyank"));
    }
}
