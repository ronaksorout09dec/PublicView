package com.skyheights.realestate.modules.maintenance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidCreateRequest {

    @NotNull(message = "Ticket ID required")
    private Long ticketId;

    @NotNull(message = "Bid amount required")
    @Positive(message = "Bid amount must be positive")
    private BigDecimal bidAmount;

    @NotNull(message = "Estimated days required")
    private Integer estimatedDays;

    private String proposal;
    private Boolean includesMaterial;
    private Integer warrantyDays;
}
