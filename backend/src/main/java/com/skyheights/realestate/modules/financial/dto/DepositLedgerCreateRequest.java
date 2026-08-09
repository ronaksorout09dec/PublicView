package com.skyheights.realestate.modules.financial.dto;

import com.skyheights.realestate.modules.financial.enums.DepositLedgerType;
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
public class DepositLedgerCreateRequest {

    @NotNull(message = "Deposit ID required")
    private Long depositId;

    @NotNull(message = "Transaction type required")
    private DepositLedgerType transactionType;

    private String description;

    @NotNull(message = "Amount required")
    private BigDecimal amount; // positive for deposit, negative for deduction? Service will handle sign

    private String referenceType; // CONDITION_REPORT, TICKET, MANUAL
    private Long referenceId;

    private String receiptS3Key;
}
