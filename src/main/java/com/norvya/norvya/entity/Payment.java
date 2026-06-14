package com.norvya.norvya.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;  // USD, XAF, EUR

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, length = 30)
    private String provider;  // STRIPE, CINETPAY, MTN_MOMO, ORANGE_MONEY

    @Column(name = "provider_tx_id", unique = true, length = 100)
    private String providerTransactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ── Enum ───────────────────────────────────────────────
    public enum Status {
        SUCCESS, FAILED, PENDING, REFUNDED
    }

}
