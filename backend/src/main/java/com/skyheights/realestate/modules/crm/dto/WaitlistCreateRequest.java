package com.skyheights.realestate.modules.crm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistCreateRequest {

    @NotNull(message = "Property ID required")
    private Long propertyId;

    @NotNull(message = "Unit type required")
    private String unitType;

    @NotNull(message = "Lead ID required")
    private Long leadId;

    private Integer priorityScore;

    private LocalDate desiredMoveIn;
}
