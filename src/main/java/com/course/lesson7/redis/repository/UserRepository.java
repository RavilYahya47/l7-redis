package com.course.lesson7.redis.repository;

import com.course.lesson7.redis.model.entity.User;
import com.course.lesson7.redis.model.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByStatus(UserStatus status);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
