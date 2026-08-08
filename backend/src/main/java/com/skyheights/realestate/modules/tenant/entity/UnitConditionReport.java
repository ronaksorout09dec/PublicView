package com.skyheights.realestate.modules.tenant.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.tenant.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "unit_condition_reports", indexes = {
    @Index(name = "idx_condition_lease", columnList = "lease_id"),
    @Index(name = "idx_condition_unit", columnList = "unit_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitConditionReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id", nullable = false)
    private LeaseAgreement lease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantProfile tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ReportType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ChecklistTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspected_by_user_id")
    private AppUser inspectedBy;

    @Column(name = "inspected_at", nullable = false)
    private Instant inspectedAt;

    @Column(name = "overall_condition", length = 30)
    private String overallCondition;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "pdf_s3_key", length = 500)
    private String pdfS3Key;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ConditionReportItem> items = new ArrayList<>();
}
