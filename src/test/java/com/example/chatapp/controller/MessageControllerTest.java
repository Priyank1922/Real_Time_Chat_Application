package com.example.chatapp.controller;

import com.example.chatapp.dto.request.UpdateMessageRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.enums.MessagePriority;
import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.exception.ResourceNotFoundException;
import com.example.chatapp.exception.UnauthorizedActionException;
import com.example.chatapp.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MessageService messageService;

    @Test
    @DisplayName("GET /api/messages/{id} - returns 200 OK")
    void testGetMessageById_Success() throws Exception {
        MessageResponse response = MessageResponse.builder()
                .id(101L)
                .roomId(10L)
                .senderId(1L)
                .senderName("priyank")
                .content("Hello World")
                .priority(MessagePriority.NORMAL)
                .build();

        when(messageService.getMessageById(101L)).thenReturn(response);

        mockMvc.perform(get("/api/messages/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.content").value("Hello World"));
    }

    @Test
    @DisplayName("GET /api/messages/{id} - not found returns 404")
    void testGetMessageById_NotFound() throws Exception {
        when(messageService.getMessageById(999L)).thenThrow(new ResourceNotFoundException("Message not found with id: 999"));

        mockMvc.perform(get("/api/messages/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/messages/{id} - returns 200 OK")
    void testUpdateMessage_Success() throws Exception {
        UpdateMessageRequest request = UpdateMessageRequest.builder()
                .userId(1L)
                .content("Updated content")
                .build();

        MessageResponse response = MessageResponse.builder()
                .id(101L)
                .roomId(10L)
                .senderId(1L)
                .senderName("priyank")
                .content("Updated content")
                .priority(MessagePriority.NORMAL)
                .edited(true)
                .build();

        when(messageService.updateMessage(eq(101L), any(UpdateMessageRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/messages/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101L))
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.edited").value(true));
    }

    @Test
    @DisplayName("PUT /api/messages/{id} - non-sender editing returns 403 Forbidden")
    void testUpdateMessage_Forbidden() throws Exception {
        UpdateMessageRequest request = UpdateMessageRequest.builder()
                .userId(2L)
                .content("Hacked content")
                .build();

        when(messageService.updateMessage(eq(101L), any(UpdateMessageRequest.class)))
                .thenThrow(new UnauthorizedActionException("Only the original sender can edit this message"));

        mockMvc.perform(put("/api/messages/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN_ACTION"));
    }

    @Test
    @DisplayName("DELETE /api/messages/{id} - returns 204 No Content")
    void testDeleteMessage_Success() throws Exception {
        doNothing().when(messageService).deleteMessage(101L, 1L);

        mockMvc.perform(delete("/api/messages/101").param("userId", "1"))
                .andExpect(status().isNoContent());
    }
}
