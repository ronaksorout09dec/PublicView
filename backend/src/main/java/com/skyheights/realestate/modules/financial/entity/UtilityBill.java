package com.skyheights.realestate.modules.financial.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "utility_bills", indexes = {
    @Index(name = "idx_bills_month", columnList = "billing_month")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UtilityBill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utility_type_id", nullable = false)
    private UtilityType utilityType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id")
    private UtilityMeter meter;

    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_units_consumed", precision = 12, scale = 2)
    private BigDecimal totalUnitsConsumed;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "bill_document_s3_key", length = 500)
    private String billDocumentS3Key;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "PENDING";
}
