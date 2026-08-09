package com.skyheights.realestate.modules.maintenance.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.maintenance.enums.WorkOrderStatus;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "work_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private MaintenanceTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorProfile vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private VendorBid bid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private AppUser assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.CREATED;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "completed_date")
    private Instant completedDate;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Column(name = "checklist_completed")
    @Builder.Default
    private Boolean checklistCompleted = false;

    @Column(name = "otp_verified_for_completion")
    @Builder.Default
    private Boolean otpVerifiedForCompletion = false;

    @Column(name = "invoice_s3_key", length = 500)
    private String invoiceS3Key;
}
