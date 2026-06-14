package com.norvya.norvya.repository;

import com.norvya.norvya.entity.Quiz;
import com.norvya.norvya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findByCourseIdOrderByCreatedAtDesc(UUID courseId);

    List<Quiz> findByUserOrderByCreatedAtDesc(User user);

    Optional<Quiz> findByIdAndUser(UUID id, User user);

    boolean existsByCourseId(UUID courseId);
}