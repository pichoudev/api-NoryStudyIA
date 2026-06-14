package com.norvya.norvya.repository;

import com.norvya.norvya.entity.FlashcardSet;
import com.norvya.norvya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashcardSetRepository extends JpaRepository<FlashcardSet, UUID> {

    List<FlashcardSet> findByCourseIdOrderByCreatedAtDesc(UUID courseId);

    List<FlashcardSet> findByUserOrderByCreatedAtDesc(User user);

    Optional<FlashcardSet> findByIdAndUser(UUID id, User user);

    boolean existsByCourseId(UUID courseId);
}