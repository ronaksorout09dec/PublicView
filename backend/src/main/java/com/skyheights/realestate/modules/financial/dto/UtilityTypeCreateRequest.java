package com.skyheights.realestate.modules.financial.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityTypeCreateRequest {

    @NotBlank(message = "Utility type name required")
    private String name; // ELECTRICITY, WATER, etc

    private String unitLabel; // kWh, KL
    private BigDecimal defaultRate;
}
