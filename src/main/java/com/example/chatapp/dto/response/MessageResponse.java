package com.example.chatapp.dto.response;

import com.example.chatapp.enums.MessagePriority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Message response representation")
public class MessageResponse {

    @Schema(example = "101")
    private Long id;

    @Schema(example = "10")
    private Long roomId;

    @Schema(example = "1")
    private Long senderId;

    @Schema(example = "priyank")
    private String senderName;

    @Schema(example = "Anyone solving today's DSA problem?")
    private String content;

    @Schema(example = "NORMAL")
    private MessagePriority priority;

    @Schema(example = "false")
    private boolean edited;

    @Schema(example = "false")
    private boolean deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public MessageResponse() {
    }

    public MessageResponse(Long id, Long roomId, Long senderId, String senderName, String content, MessagePriority priority, boolean edited, boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.priority = priority;
        this.edited = edited;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
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

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long roomId;
        private Long senderId;
        private String senderName;
        private String content;
        private MessagePriority priority;
        private boolean edited;
        private boolean deleted;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder roomId(Long roomId) {
            this.roomId = roomId;
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

        public Builder edited(boolean edited) {
            this.edited = edited;
            return this;
        }

        public Builder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public MessageResponse build() {
            return new MessageResponse(id, roomId, senderId, senderName, content, priority, edited, deleted, createdAt, updatedAt);
        }
    }
}
