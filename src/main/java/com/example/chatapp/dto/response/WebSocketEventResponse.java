package com.example.chatapp.dto.response;

import com.example.chatapp.enums.MessagePriority;
import com.example.chatapp.enums.WebSocketEventType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "WebSocket STOMP event payload broadcasted across topics")
public class WebSocketEventResponse {

    @Schema(example = "NEW_MESSAGE")
    private WebSocketEventType eventType;

    @Schema(example = "10")
    private Long roomId;

    @Schema(example = "101")
    private Long messageId;

    @Schema(example = "5")
    private Long senderId;

    @Schema(example = "Priyank")
    private String senderName;

    @Schema(example = "Anyone solving today's DSA problem?")
    private String content;

    @Schema(example = "NORMAL")
    private MessagePriority priority;

    @Schema(example = "15")
    private Long userId;

    @Schema(example = "Rahul")
    private String username;

    private LocalDateTime lastSeen;

    private LocalDateTime timestamp = LocalDateTime.now();

    public WebSocketEventResponse() {
    }

    public WebSocketEventResponse(WebSocketEventType eventType, Long roomId, Long messageId, Long senderId, String senderName, String content, MessagePriority priority, Long userId, String username, LocalDateTime lastSeen, LocalDateTime timestamp) {
        this.eventType = eventType;
        this.roomId = roomId;
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.priority = priority;
        this.userId = userId;
        this.username = username;
        this.lastSeen = lastSeen;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public WebSocketEventType getEventType() {
        return eventType;
    }

    public void setEventType(WebSocketEventType eventType) {
        this.eventType = eventType;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessagePriority getPriority() {
        return priority;
    }

    public void setPriority(MessagePriority priority) {
        this.priority = priority;
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

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private WebSocketEventType eventType;
        private Long roomId;
        private Long messageId;
        private Long senderId;
        private String senderName;
        private String content;
        private MessagePriority priority;
        private Long userId;
        private String username;
        private LocalDateTime lastSeen;
        private LocalDateTime timestamp = LocalDateTime.now();

        public Builder eventType(WebSocketEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder roomId(Long roomId) {
            this.roomId = roomId;
            return this;
        }

        public Builder messageId(Long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder senderId(Long senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder senderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder priority(MessagePriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder lastSeen(LocalDateTime lastSeen) {
            this.lastSeen = lastSeen;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public WebSocketEventResponse build() {
            return new WebSocketEventResponse(eventType, roomId, messageId, senderId, senderName, content, priority, userId, username, lastSeen, timestamp);
        }
    }
}
