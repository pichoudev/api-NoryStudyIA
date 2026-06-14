package com.norvya.norvya.repository;

import com.norvya.norvya.entity.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApiLogRepository extends JpaRepository<ApiLog, UUID> {
}