package com.skyheights.realestate.modules.financial.dto;

import com.skyheights.realestate.modules.financial.enums.InvoiceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceCreateRequest {

    @NotNull(message = "Property ID required")
    private Long propertyId;

    @NotNull(message = "Unit ID required")
    private Long unitId;

    @NotNull(message = "Tenant ID required")
    private Long tenantId;

    private Long leaseId;

    @NotNull(message = "Invoice type required")
    private InvoiceType type;

    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;

    @NotNull(message = "Issue date required")
    private LocalDate issueDate;

    @NotNull(message = "Due date required")
    private LocalDate dueDate;

    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private String notes;

    private List<LineItemRequest> lineItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineItemRequest {
        @NotNull
        private String description;
        private BigDecimal quantity;
        @NotNull
        private BigDecimal unitPrice;
        private String type; // RENT, UTILITY, LATE_FEE, etc
    }
}
