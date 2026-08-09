package com.skyheights.realestate.modules.financial.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import com.skyheights.realestate.modules.portfolio.entity.Unit;

@Entity
@Table(name = "utility_bill_splits", indexes = {
    @Index(name = "idx_splits_bill", columnList = "utility_bill_id"),
    @Index(name = "idx_splits_tenant", columnList = "tenant_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UtilityBillSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    @Builder.Default
    private String uuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utility_bill_id", nullable = false)
    private UtilityBill utilityBill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "share_ratio", nullable = false, precision = 5, scale = 4)
    private BigDecimal shareRatio;

    @Column(name = "units_allocated", precision = 12, scale = 2)
    private BigDecimal unitsAllocated;

    @Column(name = "amount_share", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountShare;

    @Column(name = "calculation_notes", columnDefinition = "TEXT")
    private String calculationNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}
