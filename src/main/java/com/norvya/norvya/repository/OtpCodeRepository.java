package com.norvya.norvya.repository;



import com.norvya.norvya.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findByEmailAndCodeAndTypeAndUsedFalse(
            String email, String code, OtpCode.OtpType type
    );

    Optional<OtpCode> findTopByEmailAndTypeOrderByCreatedAtDesc(
            String email, OtpCode.OtpType type
    );

    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.email = :email AND o.type = :type")
    void deleteAllByEmailAndType(String email, OtpCode.OtpType type);
}
