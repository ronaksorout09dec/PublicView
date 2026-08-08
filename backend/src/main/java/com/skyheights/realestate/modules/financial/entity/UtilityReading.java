package com.skyheights.realestate.modules.financial.entity;

import com.skyheights.realestate.modules.organization.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "utility_readings", indexes = {
    @Index(name = "idx_readings_meter_date", columnList = "meter_id, reading_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UtilityReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    @Builder.Default
    private String uuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    private UtilityMeter meter;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "previous_reading", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal previousReading = BigDecimal.ZERO;

    @Column(name = "current_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentReading;

    @Column(name = "rate_per_unit", nullable = false, precision = 10, scale = 4)
    private BigDecimal ratePerUnit;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    private AppUser recordedBy;

    @Column(name = "photo_s3_key", length = 500)
    private String photoS3Key;

    @Column(name = "source", length = 20)
    @Builder.Default
    private String source = "MANUAL";

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}
