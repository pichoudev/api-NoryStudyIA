package com.norvya.norvya.repository;

import com.norvya.norvya.entity.Subscription;
import com.norvya.norvya.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findTopByUserAndStatusOrderByStartedAtDesc(
            User user, Subscription.Status status
    );
}
