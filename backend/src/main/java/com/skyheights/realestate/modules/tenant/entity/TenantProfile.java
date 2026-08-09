package com.skyheights.realestate.modules.tenant.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.tenant.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tenant_profiles", indexes = {
    @Index(name = "idx_tenant_org", columnList = "org_id"),
    @Index(name = "idx_tenant_unit", columnList = "unit_id"),
    @Index(name = "idx_tenant_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantProfile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(name = "tenancy_type", length = 20)
    @Builder.Default
    private String tenancyType = "PRIMARY";

    @Column(name = "employer_name")
    private String employerName;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "monthly_income", precision = 12, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Column(name = "expected_move_out_date")
    private LocalDate expectedMoveOutDate;

    @Column(name = "actual_move_out_date")
    private LocalDate actualMoveOutDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private TenantStatus status = TenantStatus.PROSPECT;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
