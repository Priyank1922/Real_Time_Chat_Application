package com.example.chatapp.service;

import com.example.chatapp.dto.request.SendMessageRequest;
import com.example.chatapp.dto.request.UpdateMessageRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.response.WebSocketEventResponse;
import com.example.chatapp.entity.ChatMessage;
import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.User;
import com.example.chatapp.enums.MessagePriority;
import com.example.chatapp.enums.RoomMemberRole;
import com.example.chatapp.enums.WebSocketEventType;
import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.exception.ResourceNotFoundException;
import com.example.chatapp.exception.UnauthorizedActionException;
import com.example.chatapp.repository.ChatMessageRepository;
import com.example.chatapp.repository.RoomMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MessageService {

    public static final String DELETED_MESSAGE_PLACEHOLDER = "[This message has been deleted]";

    private final ChatMessageRepository chatMessageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserService userService;
    private final RoomService roomService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public MessageService(ChatMessageRepository chatMessageRepository,
                          RoomMemberRepository roomMemberRepository,
                          UserService userService,
                          RoomService roomService,
                          SimpMessagingTemplate simpMessagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userService = userService;
        this.roomService = roomService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Transactional
    public MessageResponse sendMessage(Long roomId, SendMessageRequest request) {
        ChatRoom room = roomService.getRoomEntityById(roomId);
        User sender = userService.getUserEntityById(request.getSenderId());

        // Validate membership
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndActiveTrue(roomId, sender.getId());
        if (!isMember) {
            throw new UnauthorizedActionException("User '" + sender.getUsername() + "' is not an active member of room '" + room.getName() + "' and cannot send messages.");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be blank.");
        }

        MessagePriority priority = request.getPriority() != null ? request.getPriority() : MessagePriority.NORMAL;

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent().trim())
                .priority(priority)
                .edited(false)
                .deleted(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        MessageResponse response = mapToMessageResponse(savedMessage);

        // Determine event type based on priority
        WebSocketEventType eventType = WebSocketEventType.NEW_MESSAGE;
        if (priority == MessagePriority.IMPORTANT) {
            eventType = WebSocketEventType.IMPORTANT_MESSAGE;
        } else if (priority == MessagePriority.URGENT) {
            eventType = WebSocketEventType.URGENT_MESSAGE;
        }

        WebSocketEventResponse eventResponse = WebSocketEventResponse.builder()
                .eventType(eventType)
                .roomId(roomId)
                .messageId(savedMessage.getId())
                .senderId(sender.getId())
                .senderName(sender.getUsername())
                .content(savedMessage.getContent())
                .priority(priority)
                .timestamp(savedMessage.getCreatedAt())
                .build();

        // Broadcast to room message topic
        simpMessagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", eventResponse);

        return response;
    }

    @Transactional(readOnly = true)
    public MessageResponse getMessageById(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));
        return mapToMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getRoomMessages(Long roomId, Pageable pageable) {
        // Validate room exists
        roomService.getRoomEntityById(roomId);

        Page<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
        return messages.map(this::mapToMessageResponse);
    }

    @Transactional
    public MessageResponse updateMessage(Long messageId, UpdateMessageRequest request) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        if (message.isDeleted()) {
            throw new BadRequestException("Cannot edit a deleted message.");
        }

        if (!message.getSender().getId().equals(request.getUserId())) {
            throw new UnauthorizedActionException("Only the original sender can edit this message.");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be blank.");
        }

        message.setContent(request.getContent().trim());
        message.setEdited(true);

        ChatMessage updatedMessage = chatMessageRepository.save(message);
        MessageResponse response = mapToMessageResponse(updatedMessage);

        WebSocketEventResponse eventResponse = WebSocketEventResponse.builder()
                .eventType(WebSocketEventType.MESSAGE_UPDATED)
                .roomId(updatedMessage.getRoom().getId())
                .messageId(updatedMessage.getId())
                .senderId(updatedMessage.getSender().getId())
                .senderName(updatedMessage.getSender().getUsername())
                .content(updatedMessage.getContent())
                .priority(updatedMessage.getPriority())
                .timestamp(LocalDateTime.now())
                .build();

        simpMessagingTemplate.convertAndSend("/topic/rooms/" + updatedMessage.getRoom().getId() + "/messages", eventResponse);

        return response;
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        User requester = userService.getUserEntityById(userId);

        boolean isSender = message.getSender().getId().equals(requester.getId());
        boolean isRoomOwner = message.getRoom().getCreatedBy().getId().equals(requester.getId()) ||
                roomMemberRepository.findByRoomIdAndUserIdAndActiveTrue(message.getRoom().getId(), requester.getId())
                        .map(m -> m.getRole() == RoomMemberRole.OWNER)
                        .orElse(false);

        if (!isSender && !isRoomOwner) {
            throw new UnauthorizedActionException("Only the message sender or the room owner can delete this message.");
        }

        message.setDeleted(true);
        chatMessageRepository.save(message);

        WebSocketEventResponse eventResponse = WebSocketEventResponse.builder()
                .eventType(WebSocketEventType.MESSAGE_DELETED)
                .roomId(message.getRoom().getId())
                .messageId(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getUsername())
                .content(DELETED_MESSAGE_PLACEHOLDER)
                .timestamp(LocalDateTime.now())
                .build();

        simpMessagingTemplate.convertAndSend("/topic/rooms/" + message.getRoom().getId() + "/messages", eventResponse);
    }

    public MessageResponse mapToMessageResponse(ChatMessage message) {
        String content = message.isDeleted() ? DELETED_MESSAGE_PLACEHOLDER : message.getContent();
        return MessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getUsername())
                .content(content)
                .priority(message.getPriority())
                .edited(message.isEdited())
                .deleted(message.isDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
