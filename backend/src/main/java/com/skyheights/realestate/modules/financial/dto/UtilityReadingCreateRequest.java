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
public class UtilityReadingCreateRequest {

    @NotNull(message = "Meter ID required")
    private Long meterId;

    @NotNull(message = "Reading date required")
    private LocalDate readingDate;

    private BigDecimal previousReading;
    @NotNull(message = "Current reading required")
    private BigDecimal currentReading;

    @NotNull(message = "Rate per unit required")
    private BigDecimal ratePerUnit;

    private String photoS3Key;
}
