package com.skyheights.realestate.modules.financial.dto;

import com.skyheights.realestate.modules.financial.enums.InvoiceStatus;
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
public class InvoiceUpdateRequest {

    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal amountPaid;
    private InvoiceStatus status;
    private String notes;
    private List<InvoiceCreateRequest.LineItemRequest> lineItems;
}
