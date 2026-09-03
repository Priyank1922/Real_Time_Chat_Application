package com.example.chatapp.dto.response;

import com.example.chatapp.enums.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Chat room response representation")
public class RoomResponse {

    @Schema(example = "10")
    private Long id;

    @Schema(example = "Java Placement Preparation")
    private String name;

    @Schema(example = "Discussion room for Java, Spring Boot and DSA preparation")
    private String description;

    @Schema(example = "STUDY")
    private RoomType roomType;

    @Schema(example = "1")
    private Long createdById;

    @Schema(example = "priyank")
    private String createdByName;

    @Schema(example = "5")
    private long memberCount;

    @Schema(example = "true")
    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public RoomResponse() {
    }

    public RoomResponse(Long id, String name, String description, RoomType roomType, Long createdById, String createdByName, long memberCount, boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.roomType = roomType;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.memberCount = memberCount;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(long memberCount) {
        this.memberCount = memberCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
        private String name;
        private String description;
        private RoomType roomType;
        private Long createdById;
        private String createdByName;
        private long memberCount;
        private boolean active = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder roomType(RoomType roomType) {
            this.roomType = roomType;
            return this;
        }

        public Builder createdById(Long createdById) {
            this.createdById = createdById;
            return this;
        }

        public Builder createdByName(String createdByName) {
            this.createdByName = createdByName;
            return this;
        }

        public Builder memberCount(long memberCount) {
            this.memberCount = memberCount;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
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

        public RoomResponse build() {
            return new RoomResponse(id, name, description, roomType, createdById, createdByName, memberCount, active, createdAt, updatedAt);
        }
    }
}
