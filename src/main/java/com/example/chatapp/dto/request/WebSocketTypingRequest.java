package com.example.chatapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for typing indicators via WebSocket STOMP")
public class WebSocketTypingRequest {

    @NotNull(message = "User ID is required")
    @Schema(example = "1", description = "User ID of user who is typing / stopped typing")
    private Long userId;

    @Schema(example = "Rahul", description = "Username of user typing")
    private String username;

    public WebSocketTypingRequest() {
    }

    public WebSocketTypingRequest(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String username;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public WebSocketTypingRequest build() {
            return new WebSocketTypingRequest(userId, username);
        }
    }
}
