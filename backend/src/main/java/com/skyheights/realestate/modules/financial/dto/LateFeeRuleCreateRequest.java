package com.skyheights.realestate.modules.financial.dto;

import com.skyheights.realestate.modules.financial.enums.LateFeeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LateFeeRuleCreateRequest {

    private Long propertyId; // null = org-wide

    @NotBlank(message = "Name required")
    private String name;

    @NotNull(message = "Fee type required")
    private LateFeeType feeType;

    private BigDecimal amountValue; // for FIXED
    private BigDecimal percentageRate; // for PERCENTAGE_PER_DAY

    private Integer gracePeriodDays;
    private BigDecimal maxCapAmount;
    private Boolean compounding;
    private Boolean isActive;
}
