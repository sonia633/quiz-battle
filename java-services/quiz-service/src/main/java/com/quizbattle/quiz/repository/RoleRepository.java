package com.quizbattle.quiz.repository;

import com.quizbattle.quiz.entity.Role;
import com.quizbattle.quiz.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
