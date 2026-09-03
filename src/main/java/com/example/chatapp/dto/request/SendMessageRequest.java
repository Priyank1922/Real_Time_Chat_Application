package com.example.chatapp.dto.request;

import com.example.chatapp.enums.MessagePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to send a message to a room via REST")
public class SendMessageRequest {

    @NotNull(message = "Sender ID is required")
    @Schema(example = "1", description = "User ID of the message sender")
    private Long senderId;

    @NotBlank(message = "Message content cannot be blank")
    @Size(min = 1, max = 4000, message = "Message content must be between 1 and 4000 characters")
    @Schema(example = "Anyone solving today's DSA problem?", description = "Text content of the message")
    private String content;

    @Schema(example = "NORMAL", description = "Message priority: NORMAL, IMPORTANT, URGENT")
    private MessagePriority priority = MessagePriority.NORMAL;

    public SendMessageRequest() {
    }

    public SendMessageRequest(Long senderId, String content, MessagePriority priority) {
        this.senderId = senderId;
        this.content = content;
        this.priority = priority != null ? priority : MessagePriority.NORMAL;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long senderId;
        private String content;
        private MessagePriority priority = MessagePriority.NORMAL;

        public Builder senderId(Long senderId) {
            this.senderId = senderId;
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

        public SendMessageRequest build() {
            return new SendMessageRequest(senderId, content, priority);
        }
    }
}
