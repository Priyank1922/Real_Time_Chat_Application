package com.example.chatapp.entity;

import com.example.chatapp.enums.MessagePriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chat_messages",
    indexes = {
        @Index(name = "idx_chat_msg_room_id", columnList = "room_id"),
        @Index(name = "idx_chat_msg_sender_id", columnList = "sender_id"),
        @Index(name = "idx_chat_msg_created_at", columnList = "created_at"),
        @Index(name = "idx_chat_msg_priority", columnList = "priority"),
        @Index(name = "idx_chat_msg_deleted", columnList = "deleted")
    }
)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessagePriority priority = MessagePriority.NORMAL;

    @Column(nullable = false)
    private boolean edited = false;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ChatMessage() {
    }

    public ChatMessage(Long id, ChatRoom room, User sender, String content, MessagePriority priority, boolean edited, boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.room = room;
        this.sender = sender;
        this.content = content;
        this.priority = priority != null ? priority : MessagePriority.NORMAL;
        this.edited = edited;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.priority == null) {
            this.priority = MessagePriority.NORMAL;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatRoom getRoom() {
        return room;
    }

    public void setRoom(ChatRoom room) {
        this.room = room;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
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
        private ChatRoom room;
        private User sender;
        private String content;
        private MessagePriority priority = MessagePriority.NORMAL;
        private boolean edited = false;
        private boolean deleted = false;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder room(ChatRoom room) {
            this.room = room;
            return this;
        }

        public Builder sender(User sender) {
            this.sender = sender;
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

        public ChatMessage build() {
            return new ChatMessage(id, room, sender, content, priority, edited, deleted, createdAt, updatedAt);
        }
    }
}
