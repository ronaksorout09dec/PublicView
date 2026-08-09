package com.skyheights.realestate.modules.financial.dto;

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
public class UtilityBillCreateRequest {

    @NotNull(message = "Property ID required")
    private Long propertyId;

    @NotNull(message = "Utility type ID required")
    private Long utilityTypeId;

    private Long meterId; // building meter

    @NotNull(message = "Billing month required")
    private LocalDate billingMonth; // YYYY-MM-01

    @NotNull(message = "Total amount required")
    private BigDecimal totalAmount;

    private BigDecimal totalUnitsConsumed;

    private LocalDate dueDate;
    private String providerName;
    private String billDocumentS3Key;
}
