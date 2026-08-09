package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private Long propertyId;
    private String propertyName;
    private Long unitId;
    private String unitNumber;
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

    // KYC summary
    private long totalKycDocs;
    private long verifiedKycDocs;
    private boolean kycComplete;

    // Lease summary
    private Long activeLeaseId;
    private String activeLeaseNumber;
    private LocalDate activeLeaseEndDate;

    private Instant createdAt;
    private Instant updatedAt;
}
