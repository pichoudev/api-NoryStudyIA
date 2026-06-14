package com.norvya.norvya.repository;

import com.norvya.norvya.entity.QuizAttempt;
import com.norvya.norvya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findByQuizIdAndUserOrderByAttemptedAtDesc(
            UUID quizId, User user
    );

    List<QuizAttempt> findByUserOrderByAttemptedAtDesc(User user);
}