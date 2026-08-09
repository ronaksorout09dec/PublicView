package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.LeaseStatus;
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
public class LeaseUpdateRequest {

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private Integer rentDueDay;
    private Integer noticePeriodDays;
    private Integer lockInPeriodMonths;
    private BigDecimal escalationPercent;
    private LeaseStatus status;
    private String terms;
    private String terminationReason;
}
