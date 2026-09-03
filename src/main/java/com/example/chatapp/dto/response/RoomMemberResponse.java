package com.example.chatapp.dto.response;

import com.example.chatapp.enums.RoomMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Room member response representation")
public class RoomMemberResponse {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "1")
    private Long userId;

    @Schema(example = "priyank")
    private String username;

    @Schema(example = "priyank@example.com")
    private String email;

    @Schema(example = "10")
    private Long roomId;

    @Schema(example = "OWNER")
    private RoomMemberRole role;

    @Schema(example = "true")
    private boolean active;

    private LocalDateTime joinedAt;

    public RoomMemberResponse() {
    }

    public RoomMemberResponse(Long id, Long userId, String username, String email, Long roomId, RoomMemberRole role, boolean active, LocalDateTime joinedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roomId = roomId;
        this.role = role;
        this.active = active;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
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
        private Long userId;
        private String username;
        private String email;
        private Long roomId;
        private RoomMemberRole role;
        private boolean active = true;
        private LocalDateTime joinedAt;

        public Builder id(Long id) {
            this.id = id;
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

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder roomId(Long roomId) {
            this.roomId = roomId;
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

        public RoomMemberResponse build() {
            return new RoomMemberResponse(id, userId, username, email, roomId, role, active, joinedAt);
        }
    }
}
