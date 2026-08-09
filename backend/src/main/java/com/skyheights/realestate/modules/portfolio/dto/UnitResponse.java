package com.skyheights.realestate.modules.portfolio.dto;

import com.skyheights.realestate.modules.portfolio.enums.UnitStatus;
import com.skyheights.realestate.modules.portfolio.enums.UnitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long propertyId;
    private String propertyName;
    private String unitNumber;
    private Integer floor;
    private UnitType type;
    private BigDecimal sizeSqft;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private UnitStatus status;
    private String description;
    private Long currentTenantId;
    private Long currentLeaseId;
    private Set<AmenityResponse> amenities;

    private Instant createdAt;
    private Instant updatedAt;
}
