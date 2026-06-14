package com.norvya.norvya.repository;

import com.norvya.norvya.entity.FlashcardProgress;
import com.norvya.norvya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashcardProgressRepository
        extends JpaRepository<FlashcardProgress, UUID> {

    List<FlashcardProgress> findByFlashcardSetIdAndUser(
            UUID setId, User user
    );

    Optional<FlashcardProgress> findByFlashcardSetIdAndUserAndCardId(
            UUID setId, User user, String cardId
    );

    List<FlashcardProgress> findByFlashcardSetIdAndUserAndNextReviewAtBefore(
            UUID setId, User user, LocalDateTime now
    );

    // ✅ Pour le scheduler
    List<FlashcardProgress> findAllByNextReviewAtBefore(LocalDateTime now);
}