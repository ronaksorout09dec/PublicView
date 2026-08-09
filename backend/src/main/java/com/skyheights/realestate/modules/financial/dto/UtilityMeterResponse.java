package com.skyheights.realestate.modules.financial.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityMeterResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long propertyId;
    private String propertyName;
    private Long unitId;
    private String unitNumber;
    private Long utilityTypeId;
    private String utilityTypeName;
    private String meterNumber;
    private Boolean isShared;
    private String location;
    private Integer totalUnitsSharing;
    private String ratioConfig;
    private String status;
    private Instant createdAt;
}
