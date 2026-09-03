package com.example.chatapp.config;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.entity.RoomMember;
import com.example.chatapp.entity.User;
import com.example.chatapp.enums.RoomMemberRole;
import com.example.chatapp.enums.RoomType;
import com.example.chatapp.enums.UserStatus;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.RoomMemberRepository;
import com.example.chatapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;

    public DataInitializer(UserRepository userRepository,
                           ChatRoomRepository chatRoomRepository,
                           RoomMemberRepository roomMemberRepository) {
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Database is empty. Initializing demo seed data for PostgreSQL / Neon DB...");

            User priyank = userRepository.save(User.builder()
                    .username("priyank")
                    .email("priyank@example.com")
                    .password("password123")
                    .status(UserStatus.OFFLINE)
                    .build());

            User rahul = userRepository.save(User.builder()
                    .username("rahul")
                    .email("rahul@example.com")
                    .password("password123")
                    .status(UserStatus.OFFLINE)
                    .build());

            User alex = userRepository.save(User.builder()
                    .username("alex")
                    .email("alex@example.com")
                    .password("password123")
                    .status(UserStatus.OFFLINE)
                    .build());

            ChatRoom generalRoom = chatRoomRepository.save(ChatRoom.builder()
                    .name("General Discussion")
                    .description("Welcome to the real-time chat platform! Feel free to collaborate and exchange messages.")
                    .roomType(RoomType.GENERAL)
                    .createdBy(priyank)
                    .active(true)
                    .build());

            ChatRoom devRoom = chatRoomRepository.save(ChatRoom.builder()
                    .name("Engineering & Architecture")
                    .description("Technical deep-dives, Spring Boot, WebSocket, and PostgreSQL / Neon DB discussions.")
                    .roomType(RoomType.PROJECT)
                    .createdBy(priyank)
                    .active(true)
                    .build());

            roomMemberRepository.save(RoomMember.builder()
                    .room(generalRoom)
                    .user(priyank)
                    .role(RoomMemberRole.OWNER)
                    .active(true)
                    .build());

            roomMemberRepository.save(RoomMember.builder()
                    .room(generalRoom)
                    .user(rahul)
                    .role(RoomMemberRole.MEMBER)
                    .active(true)
                    .build());

            roomMemberRepository.save(RoomMember.builder()
                    .room(devRoom)
                    .user(priyank)
                    .role(RoomMemberRole.OWNER)
                    .active(true)
                    .build());

            log.info("Demo seed data created successfully (Demo users: priyank, rahul, alex with password: password123)");
        }
    }
}
