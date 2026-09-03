package com.example.chatapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to edit an existing message")
public class UpdateMessageRequest {

    @NotNull(message = "User ID is required")
    @Schema(example = "1", description = "User ID of the sender editing the message")
    private Long userId;

    @NotBlank(message = "Message content cannot be blank")
    @Size(min = 1, max = 4000, message = "Message content must be between 1 and 4000 characters")
    @Schema(example = "Updated message content", description = "New content of the message")
    private String content;

    public UpdateMessageRequest() {
    }

    public UpdateMessageRequest(Long userId, String content) {
        this.userId = userId;
        this.content = content;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String content;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public UpdateMessageRequest build() {
            return new UpdateMessageRequest(userId, content);
        }
    }
}
