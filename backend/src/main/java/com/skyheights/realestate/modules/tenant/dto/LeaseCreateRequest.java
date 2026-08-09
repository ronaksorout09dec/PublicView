package com.skyheights.realestate.modules.tenant.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaseCreateRequest {

    @NotNull(message = "Property ID required")
    private Long propertyId;

    @NotNull(message = "Unit ID required")
    private Long unitId;

    @NotNull(message = "Tenant ID required")
    private Long tenantId;

    @NotNull(message = "Start date required")
    private LocalDate startDate;

    @NotNull(message = "End date required")
    private LocalDate endDate;

    @NotNull(message = "Rent amount required")
    private BigDecimal rentAmount;

    @NotNull(message = "Deposit amount required")
    private BigDecimal depositAmount;

    @Min(value = 1, message = "Rent due day must be 1-28")
    @Builder.Default
    private Integer rentDueDay = 5;

    private Integer noticePeriodDays;
    private Integer lockInPeriodMonths;

    private BigDecimal escalationPercent;

    private String terms;

    private Long parentLeaseId; // for renewal
}
