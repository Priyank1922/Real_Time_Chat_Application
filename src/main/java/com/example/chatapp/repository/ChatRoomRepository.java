package com.example.chatapp.repository;

import com.example.chatapp.entity.ChatRoom;
import com.example.chatapp.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByActiveTrue();

    Optional<ChatRoom> findByIdAndActiveTrue(Long id);

    List<ChatRoom> findByRoomTypeAndActiveTrue(RoomType roomType);

    List<ChatRoom> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    @Query("SELECT r FROM ChatRoom r WHERE r.active = true " +
           "AND (:roomType IS NULL OR r.roomType = :roomType) " +
           "AND (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<ChatRoom> searchRooms(@Param("search") String search, @Param("roomType") RoomType roomType);
}
