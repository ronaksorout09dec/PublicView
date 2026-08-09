package com.skyheights.realestate.modules.financial.dto;

import com.skyheights.realestate.modules.financial.enums.DepositStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityDepositResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long leaseId;
    private String leaseNumber;
    private Long tenantId;
    private String tenantName;
    private Long unitId;
    private String unitNumber;
    private BigDecimal totalDeposited;
    private String currency;
    private DepositStatus status;
    private String heldInAccount;
    private Instant createdAt;

    private BigDecimal currentBalance;
    private List<LedgerResponse> ledgerEntries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LedgerResponse {
        private Long id;
        private String uuid;
        private String transactionType;
        private String description;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String referenceType;
        private Long referenceId;
        private Long createdByUserId;
        private String createdByName;
        private String receiptS3Key;
        private String receiptPresignedUrl;
        private Instant createdAt;
    }
}
