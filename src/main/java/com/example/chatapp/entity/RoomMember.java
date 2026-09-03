package com.example.chatapp.entity;

import com.example.chatapp.enums.RoomMemberRole;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "room_members",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_user", columnNames = {"room_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_room_member_room_id", columnList = "room_id"),
        @Index(name = "idx_room_member_user_id", columnList = "user_id"),
        @Index(name = "idx_room_member_active", columnList = "active")
    }
)
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomMemberRole role = RoomMemberRole.MEMBER;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public RoomMember() {
    }

    public RoomMember(Long id, ChatRoom room, User user, RoomMemberRole role, boolean active, LocalDateTime joinedAt) {
        this.id = id;
        this.room = room;
        this.user = user;
        this.role = role != null ? role : RoomMemberRole.MEMBER;
        this.active = active;
        this.joinedAt = joinedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public RoomMemberRole getRole() {
        return role;
    }

    public void setRole(RoomMemberRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private ChatRoom room;
        private User user;
        private RoomMemberRole role = RoomMemberRole.MEMBER;
        private boolean active = true;
        private LocalDateTime joinedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder room(ChatRoom room) {
            this.room = room;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder role(RoomMemberRole role) {
            this.role = role;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder joinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
            return this;
        }

        public RoomMember build() {
            return new RoomMember(id, room, user, role, active, joinedAt);
        }
    }
}
