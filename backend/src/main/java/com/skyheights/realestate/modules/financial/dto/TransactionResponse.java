package com.skyheights.realestate.modules.financial.dto;

import com.skyheights.realestate.modules.financial.enums.TransactionCategory;
import com.skyheights.realestate.modules.financial.enums.TransactionType;
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
public class TransactionResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long propertyId;
    private String propertyName;
    private Long unitId;
    private String unitNumber;
    private TransactionType type;
    private TransactionCategory category;
    private BigDecimal amount;
    private LocalDate date;
    private String description;
    private String paymentMethod;
    private Long invoiceId;
    private String invoiceNumber;
    private Long vendorPayoutId;
    private String ledgerReferenceType;
    private Long ledgerReferenceId;
    private String receiptS3Key;
    private String receiptPresignedUrl;
    private Long createdByUserId;
    private String createdByName;
    private Instant createdAt;
}
