package com.quizbattle.quiz.repository;

import com.quizbattle.quiz.entity.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {

    Optional<GameRoom> findByCode(String code);

    boolean existsByCode(String code);
}
