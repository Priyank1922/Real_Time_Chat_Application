package com.example.chatapp.service;

import com.example.chatapp.config.CacheConfig;
import com.example.chatapp.dto.request.CreateRoomRequest;
import com.example.chatapp.dto.request.UpdateRoomRequest;
import com.example.chatapp.dto.response.RoomMemberResponse;
import com.example.chatapp.dto.response.RoomResponse;
import com.example.chatapp.dto.response.WebSocketEventResponse;
import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RoomMember;
import com.example.chatapp.entity.User;
import com.example.chatapp.enums.RoomMemberRole;
import com.example.chatapp.enums.RoomType;
import com.example.chatapp.enums.WebSocketEventType;
import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.exception.ConflictException;
import com.example.chatapp.exception.ResourceNotFoundException;
import com.example.chatapp.exception.UnauthorizedActionException;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.RoomMemberRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserService userService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public RoomService(ChatRoomRepository chatRoomRepository,
                       RoomMemberRepository roomMemberRepository,
                       UserService userService,
                       SimpMessagingTemplate simpMessagingTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userService = userService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.ROOMS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.USER_ROOMS_CACHE, key = "#request.userId")
    })
    public RoomResponse createRoom(CreateRoomRequest request) {
        User creator = userService.getUserEntityById(request.getUserId());

        ChatRoom room = ChatRoom.builder()
                .name(request.getName())
                .description(request.getDescription())
                .roomType(request.getRoomType())
                .createdBy(creator)
                .active(true)
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(room);

        // Creator automatically becomes room OWNER
        RoomMember ownerMember = RoomMember.builder()
                .room(savedRoom)
                .user(creator)
                .role(RoomMemberRole.OWNER)
                .active(true)
                .joinedAt(LocalDateTime.now())
                .build();

        roomMemberRepository.save(ownerMember);

        return mapToRoomResponse(savedRoom, 1L);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.ROOM_DETAILS_CACHE, key = "#roomId")
    public RoomResponse getRoomById(Long roomId) {
        ChatRoom room = getRoomEntityById(roomId);
        long memberCount = roomMemberRepository.countByRoomIdAndActiveTrue(roomId);
        return mapToRoomResponse(room, memberCount);
    }

    @Transactional(readOnly = true)
    public ChatRoom getRoomEntityById(Long roomId) {
        return chatRoomRepository.findByIdAndActiveTrue(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + roomId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.ROOMS_CACHE, key = "'all_' + (#search != null ? #search : '') + '_' + (#roomType != null ? #roomType : '')")
    public List<RoomResponse> getRooms(String search, RoomType roomType) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        String cleanSearch = hasSearch ? search.trim() : null;

        List<ChatRoom> rooms;
        if (cleanSearch == null && roomType == null) {
            rooms = chatRoomRepository.findByActiveTrue();
        } else if (cleanSearch == null) {
            rooms = chatRoomRepository.findByRoomTypeAndActiveTrue(roomType);
        } else if (roomType == null) {
            rooms = chatRoomRepository.searchByTerm(cleanSearch);
        } else {
            rooms = chatRoomRepository.searchByTermAndRoomType(cleanSearch, roomType);
        }

        return rooms.stream()
                .map(room -> {
                    long count = roomMemberRepository.countByRoomIdAndActiveTrue(room.getId());
                    return mapToRoomResponse(room, count);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.ROOMS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.ROOM_DETAILS_CACHE, key = "#roomId")
    })
    public RoomResponse updateRoom(Long roomId, UpdateRoomRequest request) {
        ChatRoom room = getRoomEntityById(roomId);
        User requester = userService.getUserEntityById(request.getUserId());

        validateRoomOwner(room, requester.getId());

        if (request.getName() != null && !request.getName().isBlank()) {
            room.setName(request.getName());
        }
        if (request.getDescription() != null) {
            room.setDescription(request.getDescription());
        }
        if (request.getRoomType() != null) {
            room.setRoomType(request.getRoomType());
        }

        ChatRoom updatedRoom = chatRoomRepository.save(room);
        long count = roomMemberRepository.countByRoomIdAndActiveTrue(roomId);
        return mapToRoomResponse(updatedRoom, count);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.ROOMS_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.ROOM_DETAILS_CACHE, key = "#roomId"),
        @CacheEvict(value = CacheConfig.ROOM_MEMBERS_CACHE, key = "#roomId")
    })
    public void deleteRoom(Long roomId, Long userId) {
        ChatRoom room = getRoomEntityById(roomId);
        User requester = userService.getUserEntityById(userId);

        validateRoomOwner(room, requester.getId());

        room.setActive(false);
        chatRoomRepository.save(room);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.ROOM_MEMBERS_CACHE, key = "#roomId"),
        @CacheEvict(value = CacheConfig.ROOM_DETAILS_CACHE, key = "#roomId"),
        @CacheEvict(value = CacheConfig.USER_ROOMS_CACHE, key = "#userId")
    })
    public RoomMemberResponse joinRoom(Long roomId, Long userId) {
        ChatRoom room = getRoomEntityById(roomId);
        User user = userService.getUserEntityById(userId);

        Optional<RoomMember> existingMemberOpt = roomMemberRepository.findByRoomIdAndUserId(roomId, userId);
        RoomMember member;

        if (existingMemberOpt.isPresent()) {
            member = existingMemberOpt.get();
            if (member.isActive()) {
                throw new ConflictException("User '" + user.getUsername() + "' is already an active member of room '" + room.getName() + "'");
            }
            member.setActive(true);
            member.setJoinedAt(LocalDateTime.now());
        } else {
            member = RoomMember.builder()
                    .room(room)
                    .user(user)
                    .role(RoomMemberRole.MEMBER)
                    .active(true)
                    .joinedAt(LocalDateTime.now())
                    .build();
        }

        RoomMember savedMember = roomMemberRepository.save(member);

        // Broadcast ROOM_USER_JOINED event
        WebSocketEventResponse joinEvent = WebSocketEventResponse.builder()
                .eventType(WebSocketEventType.ROOM_USER_JOINED)
                .roomId(roomId)
                .userId(user.getId())
                .username(user.getUsername())
                .timestamp(LocalDateTime.now())
                .build();

        simpMessagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", joinEvent);
        simpMessagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", joinEvent);

        return mapToRoomMemberResponse(savedMember);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.ROOM_MEMBERS_CACHE, key = "#roomId"),
        @CacheEvict(value = CacheConfig.ROOM_DETAILS_CACHE, key = "#roomId"),
        @CacheEvict(value = CacheConfig.USER_ROOMS_CACHE, key = "#userId")
    })
    public void leaveRoom(Long roomId, Long userId) {
        ChatRoom room = getRoomEntityById(roomId);
        User user = userService.getUserEntityById(userId);

        RoomMember member = roomMemberRepository.findByRoomIdAndUserIdAndActiveTrue(roomId, userId)
                .orElseThrow(() -> new BadRequestException("User '" + user.getUsername() + "' is not an active member of room '" + room.getName() + "'"));

        member.setActive(false);
        roomMemberRepository.save(member);

        // Broadcast ROOM_USER_LEFT event
        WebSocketEventResponse leaveEvent = WebSocketEventResponse.builder()
                .eventType(WebSocketEventType.ROOM_USER_LEFT)
                .roomId(roomId)
                .userId(user.getId())
                .username(user.getUsername())
                .timestamp(LocalDateTime.now())
                .build();

        simpMessagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events", leaveEvent);
        simpMessagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", leaveEvent);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.ROOM_MEMBERS_CACHE, key = "#roomId")
    public List<RoomMemberResponse> getRoomMembers(Long roomId) {
        // Validate room existence
        getRoomEntityById(roomId);

        return roomMemberRepository.findByRoomIdAndActiveTrue(roomId).stream()
                .map(this::mapToRoomMemberResponse)
                .collect(Collectors.toList());
    }

    public boolean isUserActiveMember(Long roomId, Long userId) {
        return roomMemberRepository.existsByRoomIdAndUserIdAndActiveTrue(roomId, userId);
    }

    private void validateRoomOwner(ChatRoom room, Long userId) {
        boolean isOwner = room.getCreatedBy().getId().equals(userId) ||
                roomMemberRepository.findByRoomIdAndUserIdAndActiveTrue(room.getId(), userId)
                        .map(m -> m.getRole() == RoomMemberRole.OWNER)
                        .orElse(false);

        if (!isOwner) {
            throw new UnauthorizedActionException("Only the room owner can perform this operation on room '" + room.getName() + "'");
        }
    }

    public RoomResponse mapToRoomResponse(ChatRoom room, long memberCount) {
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .roomType(room.getRoomType())
                .createdById(room.getCreatedBy().getId())
                .createdByName(room.getCreatedBy().getUsername())
                .memberCount(memberCount)
                .active(room.isActive())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    public RoomMemberResponse mapToRoomMemberResponse(RoomMember member) {
        return RoomMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .email(member.getUser().getEmail())
                .roomId(member.getRoom().getId())
                .role(member.getRole())
                .active(member.isActive())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
