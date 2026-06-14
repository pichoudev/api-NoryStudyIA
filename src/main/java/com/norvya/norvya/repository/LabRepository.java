package com.norvya.norvya.repository;

import com.norvya.norvya.entity.Lab;
import com.norvya.norvya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabRepository extends JpaRepository<Lab, UUID> {

    List<Lab> findByCourseIdOrderByCreatedAtDesc(UUID courseId);

    List<Lab> findByUserOrderByCreatedAtDesc(User user);

    Optional<Lab> findByIdAndUser(UUID id, User user);

    boolean existsByCourseId(UUID courseId);
}