package com.example.chatapp.controller;

import com.example.chatapp.dto.request.CreateUserRequest;
import com.example.chatapp.dto.request.LoginRequest;
import com.example.chatapp.dto.request.UpdateUserRequest;
import com.example.chatapp.dto.response.RoomResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.enums.RoomType;
import com.example.chatapp.enums.UserStatus;
import com.example.chatapp.exception.ConflictException;
import com.example.chatapp.exception.ResourceNotFoundException;
import com.example.chatapp.exception.UnauthorizedActionException;
import com.example.chatapp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/users - returns 201 Created")
    void testCreateUser_ReturnsCreated() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("priyank")
                .email("priyank@example.com")
                .password("password123")
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("priyank")
                .email("priyank@example.com")
                .status(UserStatus.OFFLINE)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("priyank"))
                .andExpect(jsonPath("$.email").value("priyank@example.com"))
                .andExpect(jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    @DisplayName("POST /api/users/login - returns 200 OK with valid credentials")
    void testLogin_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("priyank")
                .password("password123")
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("priyank")
                .email("priyank@example.com")
                .status(UserStatus.ONLINE)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("priyank"));
    }

    @Test
    @DisplayName("POST /api/users/login - invalid credentials returns 403 Forbidden")
    void testLogin_InvalidCredentials_Returns403() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("priyank")
                .password("wrongpassword")
                .build();

        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedActionException("Invalid username/email or password"));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN_ACTION"))
                .andExpect(jsonPath("$.message").value("Invalid username/email or password"));
    }

    @Test
    @DisplayName("POST /api/users - invalid payload returns 400 Bad Request")
    void testCreateUser_InvalidPayload_Returns400() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("")
                .email("invalid-email")
                .password("")
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/users - duplicate username returns 409 Conflict")
    void testCreateUser_Duplicate_Returns409() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("priyank")
                .email("priyank@example.com")
                .password("password123")
                .build();

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new ConflictException("Username 'priyank' is already taken"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Username 'priyank' is already taken"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - returns 200 OK")
    void testGetUserById_Success() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("priyank")
                .email("priyank@example.com")
                .status(UserStatus.ONLINE)
                .build();

        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("priyank"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - not found returns 404 Not Found")
    void testGetUserById_NotFound() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/users/{id}/rooms - returns user rooms")
    void testGetUserRooms_Success() throws Exception {
        RoomResponse roomResponse = RoomResponse.builder()
                .id(10L)
                .name("Java Placement Preparation")
                .roomType(RoomType.STUDY)
                .createdById(1L)
                .createdByName("priyank")
                .memberCount(1L)
                .active(true)
                .build();

        when(userService.getUserRooms(1L)).thenReturn(List.of(roomResponse));

        mockMvc.perform(get("/api/users/1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].name").value("Java Placement Preparation"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - returns 204 No Content")
    void testDeleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }
}
