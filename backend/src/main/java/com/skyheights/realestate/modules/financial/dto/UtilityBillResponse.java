package com.skyheights.realestate.modules.financial.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityBillResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long propertyId;
    private String propertyName;
    private Long utilityTypeId;
    private String utilityTypeName;
    private Long meterId;
    private String meterNumber;
    private LocalDate billingMonth;
    private BigDecimal totalAmount;
    private BigDecimal totalUnitsConsumed;
    private LocalDate dueDate;
    private String providerName;
    private String billDocumentS3Key;
    private String billPresignedUrl;
    private String status;
    private Instant createdAt;

    private List<SplitResponse> splits;
    private int splitsCount;
    private BigDecimal totalSplitAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SplitResponse {
        private Long id;
        private String uuid;
        private Long tenantId;
        private String tenantName;
        private Long unitId;
        private String unitNumber;
        private BigDecimal shareRatio;
        private BigDecimal unitsAllocated;
        private BigDecimal amountShare;
        private Long invoiceId;
        private String invoiceNumber;
        private String calculationNotes;
    }
}
