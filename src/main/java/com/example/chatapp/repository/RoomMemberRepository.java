package com.example.chatapp.repository;

import com.example.chatapp.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    List<RoomMember> findByRoomIdAndActiveTrue(Long roomId);

    List<RoomMember> findByUserIdAndActiveTrue(Long userId);

    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    Optional<RoomMember> findByRoomIdAndUserIdAndActiveTrue(Long roomId, Long userId);

    boolean existsByRoomIdAndUserIdAndActiveTrue(Long roomId, Long userId);

    long countByRoomIdAndActiveTrue(Long roomId);
}
