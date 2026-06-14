package com.norvya.norvya.repository;

import com.norvya.norvya.entity.Exercise;
import com.norvya.norvya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findByCourseIdOrderByCreatedAtDesc(UUID courseId);

    List<Exercise> findByUserOrderByCreatedAtDesc(User user);

    Optional<Exercise> findByIdAndUser(UUID id, User user);

    boolean existsByCourseId(UUID courseId);
}