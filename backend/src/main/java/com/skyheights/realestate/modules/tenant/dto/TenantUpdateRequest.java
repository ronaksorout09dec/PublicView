package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.TenantStatus;
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
public class TenantUpdateRequest {

    private Long propertyId;
    private Long unitId;
    private String tenancyType;
    private String employerName;
    private String occupation;
    private BigDecimal monthlyIncome;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private LocalDate moveInDate;
    private LocalDate expectedMoveOutDate;
    private LocalDate actualMoveOutDate;
    private TenantStatus status;
    private String notes;
}
