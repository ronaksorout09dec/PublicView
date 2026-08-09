package com.skyheights.realestate.modules.tenant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "condition_photos", indexes = {
    @Index(name = "idx_cond_photos_report", columnList = "report_id"),
    @Index(name = "idx_cond_photos_item", columnList = "report_item_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConditionPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    @Builder.Default
    private String uuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_item_id")
    private ConditionReportItem reportItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private UnitConditionReport report;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "caption", length = 500)
    private String caption;

    @Column(name = "taken_at")
    private Instant takenAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}
