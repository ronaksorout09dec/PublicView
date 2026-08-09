package com.skyheights.realestate.modules.tenant.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "condition_report_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConditionReportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private UnitConditionReport report;

    @Column(name = "area", nullable = false, length = 100)
    private String area;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "condition", nullable = false, length = 30)
    private String condition;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_repair_cost", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal estimatedRepairCost = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @OneToMany(mappedBy = "reportItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConditionPhoto> photos = new ArrayList<>();
}
