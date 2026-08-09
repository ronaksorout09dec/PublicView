package com.skyheights.realestate.modules.financial.dto;

import com.skyheights.realestate.modules.financial.enums.TransactionCategory;
import com.skyheights.realestate.modules.financial.enums.TransactionType;
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
public class TransactionCreateRequest {

    private Long propertyId;
    private Long unitId;

    @NotNull(message = "Transaction type required")
    private TransactionType type;

    @NotNull(message = "Category required")
    private TransactionCategory category;

    @NotNull(message = "Amount required")
    private BigDecimal amount;

    @NotNull(message = "Date required")
    private LocalDate date;

    private String description;
    private String paymentMethod; // CASH, UPI, BANK_TRANSFER, CHEQUE, ONLINE

    private Long invoiceId;
    private Long vendorPayoutId;

    private String ledgerReferenceType;
    private Long ledgerReferenceId;

    private String receiptS3Key;
}
