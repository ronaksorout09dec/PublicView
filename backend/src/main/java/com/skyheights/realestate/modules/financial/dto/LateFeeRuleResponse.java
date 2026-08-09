package com.skyheights.realestate.modules.financial.dto;

import com.skyheights.realestate.modules.financial.enums.LateFeeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LateFeeRuleResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long propertyId;
    private String propertyName;
    private String name;
    private LateFeeType feeType;
    private BigDecimal amountValue;
    private BigDecimal percentageRate;
    private Integer gracePeriodDays;
    private BigDecimal maxCapAmount;
    private Boolean compounding;
    private Boolean isActive;
    private Instant createdAt;
}
