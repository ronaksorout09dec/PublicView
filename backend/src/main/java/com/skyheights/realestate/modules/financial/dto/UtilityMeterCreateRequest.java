package com.skyheights.realestate.modules.financial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityMeterCreateRequest {

    @NotNull(message = "Property ID required")
    private Long propertyId;

    private Long unitId; // null = master/building meter

    @NotNull(message = "Utility type ID required")
    private Long utilityTypeId;

    @NotBlank(message = "Meter number required")
    private String meterNumber;

    private Boolean isShared;
    private String location;
    private Integer totalUnitsSharing;
    private String ratioConfig; // JSON string e.g {"type":"EQUAL"} or {"type":"RATIO","ratios":{"101":0.5}}
}
