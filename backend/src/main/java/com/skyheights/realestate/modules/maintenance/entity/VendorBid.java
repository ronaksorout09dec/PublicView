package com.skyheights.realestate.modules.maintenance.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.maintenance.enums.BidStatus;
import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vendor_bids", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ticket_vendor", columnNames = {"ticket_id","vendor_id"})
}, indexes = {
    @Index(name = "idx_bids_ticket", columnList = "ticket_id, status"),
    @Index(name = "idx_bids_vendor", columnList = "vendor_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorBid extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private MaintenanceTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorProfile vendor;

    @Column(name = "bid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal bidAmount;

    @Column(name = "estimated_days", nullable = false)
    private Integer estimatedDays;

    @Column(name = "proposal", columnDefinition = "TEXT")
    private String proposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private BidStatus status = BidStatus.SUBMITTED;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "includes_material")
    @Builder.Default
    private Boolean includesMaterial = false;

    @Column(name = "warranty_days")
    @Builder.Default
    private Integer warrantyDays = 0;
}
