package com.example.chatapp.service;

import com.example.chatapp.config.CacheConfig;
import com.example.chatapp.dto.request.CreateUserRequest;
import com.example.chatapp.dto.request.LoginRequest;
import com.example.chatapp.dto.request.UpdateUserRequest;
import com.example.chatapp.dto.response.RoomResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.entity.RoomMember;
import com.example.chatapp.entity.User;
import com.example.chatapp.enums.UserStatus;
import com.example.chatapp.exception.ConflictException;
import com.example.chatapp.exception.ResourceNotFoundException;
import com.example.chatapp.exception.UnauthorizedActionException;
import com.example.chatapp.repository.RoomMemberRepository;
import com.example.chatapp.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;

    public UserService(UserRepository userRepository, RoomMemberRepository roomMemberRepository) {
        this.userRepository = userRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email '" + request.getEmail() + "' is already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .status(UserStatus.OFFLINE)
                .build();

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        String identifier = request.getUsername().trim();
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new UnauthorizedActionException("Invalid username/email or password"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new UnauthorizedActionException("Invalid username/email or password");
        }

        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = getUserEntityById(id);
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = CacheConfig.USER_ROOMS_CACHE, key = "#id")
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = getUserEntityById(id);

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (userRepository.existsByUsernameAndIdNot(request.getUsername(), id)) {
                throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new ConflictException("Email '" + request.getEmail() + "' is already taken");
            }
            user.setEmail(request.getEmail());
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.USER_ROOMS_CACHE, key = "#id")
    public void deleteUser(Long id) {
        User user = getUserEntityById(id);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.USER_ROOMS_CACHE, key = "#userId")
    public List<RoomResponse> getUserRooms(Long userId) {
        // Validate user existence
        getUserEntityById(userId);

        List<RoomMember> memberships = roomMemberRepository.findByUserIdAndActiveTrue(userId);
        return memberships.stream()
                .filter(m -> m.getRoom().isActive())
                .map(m -> {
                    long count = roomMemberRepository.countByRoomIdAndActiveTrue(m.getRoom().getId());
                    return RoomResponse.builder()
                            .id(m.getRoom().getId())
                            .name(m.getRoom().getName())
                            .description(m.getRoom().getDescription())
                            .roomType(m.getRoom().getRoomType())
                            .createdById(m.getRoom().getCreatedBy().getId())
                            .createdByName(m.getRoom().getCreatedBy().getUsername())
                            .memberCount(count)
                            .active(m.getRoom().isActive())
                            .createdAt(m.getRoom().getCreatedAt())
                            .updatedAt(m.getRoom().getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .lastSeen(user.getLastSeen())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
