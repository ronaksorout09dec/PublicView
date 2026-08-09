package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.LeaseStatus;
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
public class LeaseResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long propertyId;
    private String propertyName;
    private Long unitId;
    private String unitNumber;
    private Long tenantId;
    private String tenantName;
    private String tenantEmail;
    private String leaseNumber;
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
    private String finalPdfS3Key;
    private String finalPdfPresignedUrl;
    private Integer leaseVersion;
    private Long parentLeaseId;
    private String terminationReason;
    private long esignTotal;
    private long esignSigned;
    private boolean allSigned;

    private Instant createdAt;
    private Instant updatedAt;

    // Expiry info
    private long daysUntilExpiry;
    private boolean expiringIn60Days;
    private boolean expiringIn30Days;
    private boolean expired;
}
