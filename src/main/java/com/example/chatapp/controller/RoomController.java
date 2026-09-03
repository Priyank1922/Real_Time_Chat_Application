package com.example.chatapp.controller;

import com.example.chatapp.dto.request.CreateRoomRequest;
import com.example.chatapp.dto.request.SendMessageRequest;
import com.example.chatapp.dto.request.UpdateRoomRequest;
import com.example.chatapp.dto.response.ErrorResponse;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.response.RoomMemberResponse;
import com.example.chatapp.dto.response.RoomResponse;
import com.example.chatapp.enums.RoomType;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Chat Rooms", description = "REST APIs for creating, searching, updating, joining/leaving rooms, and querying room messages")
public class RoomController {

    private final RoomService roomService;
    private final MessageService messageService;

    public RoomController(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Create a chat room", description = "Creates a new purpose-based chat room. The creator is automatically assigned as the room owner.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Room created successfully",
                content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input payload",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Creator user not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get or search chat rooms", description = "Retrieves all active chat rooms, optionally filtered by keyword and room type.")
    @ApiResponse(responseCode = "200", description = "List of rooms retrieved successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoomResponse.class))))
    public ResponseEntity<List<RoomResponse>> getRooms(
            @Parameter(description = "Keyword to search in room name or description")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by specific room type")
            @RequestParam(required = false) RoomType roomType) {
        return ResponseEntity.ok(roomService.getRooms(search, roomType));
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "Get room details by ID", description = "Retrieves details of a specific chat room.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Room found",
                content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(responseCode = "404", description = "Room not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @PutMapping("/{roomId}")
    @Operation(summary = "Update room", description = "Updates details of a room. Only the room owner is authorized to perform this operation.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Room updated successfully",
                content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid payload",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "User is not the room owner",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Room or user not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(roomId, request));
    }

    @DeleteMapping("/{roomId}")
    @Operation(summary = "Delete room", description = "Soft deletes a chat room. Only the room owner is authorized to perform this operation.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Room deleted successfully"),
        @ApiResponse(responseCode = "403", description = "User is not the room owner",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Room or user not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long roomId,
            @Parameter(description = "User ID of the requester (must be the room owner)", required = true)
            @RequestParam Long userId) {
        roomService.deleteRoom(roomId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomId}/join/{userId}")
    @Operation(summary = "Join a room", description = "Adds a user as a member of the chat room and broadcasts ROOM_USER_JOINED event.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User successfully joined the room",
                content = @Content(schema = @Schema(implementation = RoomMemberResponse.class))),
        @ApiResponse(responseCode = "404", description = "Room or user not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "User is already a member of this room",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RoomMemberResponse> joinRoom(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        RoomMemberResponse response = roomService.joinRoom(roomId, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{roomId}/leave/{userId}")
    @Operation(summary = "Leave a room", description = "Removes a user's active membership in the room and broadcasts ROOM_USER_LEFT event.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User successfully left the room"),
        @ApiResponse(responseCode = "400", description = "User is not an active member of this room",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Room or user not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> leaveRoom(
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        roomService.leaveRoom(roomId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/members")
    @Operation(summary = "Get room members", description = "Retrieves all active members in the specified chat room.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Members retrieved successfully",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoomMemberResponse.class)))),
        @ApiResponse(responseCode = "404", description = "Room not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RoomMemberResponse>> getRoomMembers(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomService.getRoomMembers(roomId));
    }

    @PostMapping("/{roomId}/messages")
    @Operation(summary = "Send a message via REST", description = "Posts a message to a chat room via REST API and broadcasts it in real-time over WebSocket.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Message sent successfully",
                content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid message payload",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "User is not a member of the room",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Room or sender not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = messageService.sendMessage(roomId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{roomId}/messages")
    @Operation(summary = "Get paginated room message history", description = "Retrieves message history for the room in paginated order.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully",
                content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "404", description = "Room not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<MessageResponse>> getRoomMessages(
            @PathVariable Long roomId,
            @ParameterObject @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(messageService.getRoomMessages(roomId, pageable));
    }
}
