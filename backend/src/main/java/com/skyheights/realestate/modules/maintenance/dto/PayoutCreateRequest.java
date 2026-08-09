package com.skyheights.realestate.modules.maintenance.dto;

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
public class PayoutCreateRequest {

    @NotNull(message = "WorkOrder ID required")
    private Long workOrderId;

    private BigDecimal tdsDeducted;
    private String paymentMethod; // UPI, BANK_TRANSFER, CASH
    private String notes;
}
