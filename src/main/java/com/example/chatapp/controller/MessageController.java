package com.example.chatapp.controller;

import com.example.chatapp.dto.request.UpdateMessageRequest;
import com.example.chatapp.dto.response.ErrorResponse;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@Tag(name = "Messages", description = "REST APIs for individual message queries, editing, and soft-deletion")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{messageId}")
    @Operation(summary = "Get message by ID", description = "Retrieves an individual message by its unique ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message found",
                content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "404", description = "Message not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> getMessageById(@PathVariable Long messageId) {
        return ResponseEntity.ok(messageService.getMessageById(messageId));
    }

    @PutMapping("/{messageId}")
    @Operation(summary = "Update message", description = "Edits message text. Only the original sender can edit the message.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message updated successfully",
                content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Cannot edit deleted or invalid message",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "User is not the message sender",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Message not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> updateMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        return ResponseEntity.ok(messageService.updateMessage(messageId, request));
    }

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Delete message (soft-delete)", description = "Soft deletes a message. Only the sender or room owner is authorized.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Message deleted successfully"),
        @ApiResponse(responseCode = "403", description = "User is neither sender nor room owner",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Message or user not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @Parameter(description = "User ID of the requester", required = true)
            @RequestParam Long userId) {
        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }
}
