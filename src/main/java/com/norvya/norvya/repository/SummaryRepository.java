package com.norvya.norvya.repository;

import com.norvya.norvya.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, UUID> {

    List<Summary> findByCourseIdOrderByCreatedAtDesc(UUID courseId);

    List<Summary> findByUserIdOrderByCreatedAtDesc(UUID userId);
}