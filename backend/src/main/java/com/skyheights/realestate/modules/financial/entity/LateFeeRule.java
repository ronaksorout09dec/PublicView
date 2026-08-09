package com.skyheights.realestate.modules.financial.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.financial.enums.LateFeeType;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "late_fee_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LateFeeRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 30)
    private LateFeeType feeType;

    @Column(name = "amount_value", precision = 12, scale = 2)
    private BigDecimal amountValue;

    @Column(name = "percentage_rate", precision = 5, scale = 2)
    private BigDecimal percentageRate;

    @Column(name = "grace_period_days")
    @Builder.Default
    private Integer gracePeriodDays = 3;

    @Column(name = "max_cap_amount", precision = 12, scale = 2)
    private BigDecimal maxCapAmount;

    @Column(name = "compounding")
    @Builder.Default
    private Boolean compounding = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
