package com.example.chatapp.dto.request;

import com.example.chatapp.enums.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to update a chat room (only owner permitted)")
public class UpdateRoomRequest {

    @NotNull(message = "User ID of the requester is required")
    @Schema(example = "1", description = "User ID of the requester (must be the room owner)")
    private Long userId;

    @Size(min = 2, max = 100, message = "Room name must be between 2 and 100 characters")
    @Schema(example = "Advanced Java Placement Preparation", description = "Updated name of the room")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(example = "Updated description for Java preparation", description = "Updated description of the room")
    private String description;

    @Schema(example = "STUDY", description = "Updated room type")
    private RoomType roomType;

    public UpdateRoomRequest() {
    }

    public UpdateRoomRequest(Long userId, String name, String description, RoomType roomType) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.roomType = roomType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String name;
        private String description;
        private RoomType roomType;

        public Builder userId(Long userId) {
            this.userId = userId;
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

        public UpdateRoomRequest build() {
            return new UpdateRoomRequest(userId, name, description, roomType);
        }
    }
}
