package com.example.chatapp.dto.request;

import com.example.chatapp.enums.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to create a chat room")
public class CreateRoomRequest {

    @NotBlank(message = "Room name cannot be blank")
    @Size(min = 2, max = 100, message = "Room name must be between 2 and 100 characters")
    @Schema(example = "Java Placement Preparation", description = "Name of the room")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(example = "Discussion room for Java, Spring Boot and DSA preparation", description = "Description of the room")
    private String description;

    @NotNull(message = "Room type is required")
    @Schema(example = "STUDY", description = "Type of context-based chat room")
    private RoomType roomType;

    @NotNull(message = "User ID of the creator is required")
    @Schema(example = "1", description = "User ID of the creator who becomes the room owner")
    private Long userId;

    public CreateRoomRequest() {
    }

    public CreateRoomRequest(String name, String description, RoomType roomType, Long userId) {
        this.name = name;
        this.description = description;
        this.roomType = roomType;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private RoomType roomType;
        private Long userId;

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

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public CreateRoomRequest build() {
            return new CreateRoomRequest(name, description, roomType, userId);
        }
    }
}
