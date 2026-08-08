package com.skyheights.realestate.modules.tenant.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.tenant.enums.LeaseStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lease_agreements", indexes = {
    @Index(name = "idx_lease_org", columnList = "org_id"),
    @Index(name = "idx_lease_unit", columnList = "unit_id"),
    @Index(name = "idx_lease_tenant", columnList = "tenant_id"),
    @Index(name = "idx_lease_end_date", columnList = "end_date")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaseAgreement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantProfile tenant;

    @Column(name = "lease_number", nullable = false, unique = true, length = 100)
    private String leaseNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "rent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "rent_due_day", nullable = false)
    @Builder.Default
    private Integer rentDueDay = 5;

    @Column(name = "notice_period_days")
    @Builder.Default
    private Integer noticePeriodDays = 30;

    @Column(name = "lock_in_period_months")
    @Builder.Default
    private Integer lockInPeriodMonths = 6;

    @Column(name = "escalation_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal escalationPercent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private LeaseStatus status = LeaseStatus.DRAFT;

    @Column(name = "terms", columnDefinition = "TEXT")
    private String terms;

    @Column(name = "final_pdf_s3_key", length = 500)
    private String finalPdfS3Key;

    @Column(name = "lease_version")
    @Builder.Default
    private Integer leaseVersion = 1; // business version, not the BaseEntity optimistic locking version

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_lease_id")
    private LeaseAgreement parentLease;

    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;

    @OneToMany(mappedBy = "lease", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EsignTracking> esignTrackings = new ArrayList<>();

    @OneToMany(mappedBy = "lease", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UnitConditionReport> conditionReports = new ArrayList<>();
}
