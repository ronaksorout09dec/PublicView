package com.skyheights.realestate.modules.maintenance.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.maintenance.enums.TicketPriority;
import com.skyheights.realestate.modules.maintenance.enums.TicketStatus;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.tenant.entity.TenantProfile;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "maintenance_tickets", indexes = {
    @Index(name = "idx_ticket_org_status", columnList = "org_id, status"),
    @Index(name = "idx_ticket_property", columnList = "property_id"),
    @Index(name = "idx_ticket_priority", columnList = "priority")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaintenanceTicket extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private TenantProfile tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by_user_id", nullable = false)
    private AppUser raisedBy;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_vendor_id")
    private VendorProfile assignedVendor;

    @Column(name = "assigned_bid_id")
    private Long assignedBidId;

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 12, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Column(name = "rating_by_tenant")
    private Integer ratingByTenant;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "sla_due_at")
    private Instant slaDueAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TicketMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VendorBid> bids = new ArrayList<>();
}
