package com.norvya.norvya.repository;

import com.norvya.norvya.entity.Course;
import com.norvya.norvya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByUserOrderByCreatedAtDesc(User user);

    Optional<Course> findByIdAndUser(UUID id, User user);
}