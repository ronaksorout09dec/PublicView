package com.skyheights.realestate.modules.portfolio.dto;

import com.skyheights.realestate.modules.portfolio.enums.PropertyStatus;
import com.skyheights.realestate.modules.portfolio.enums.PropertyType;
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
public class PropertyResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private String name;
    private PropertyType type;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer totalFloors;
    private Integer totalUnits;
    private Integer yearBuilt;
    private Long managerId;
    private String managerName;
    private PropertyStatus status;
    private String description;
    private String thumbnailS3Key;
    private Set<AmenityResponse> amenities;

    // Stats
    private long unitsCount;
    private long vacantUnitsCount;
    private long occupiedUnitsCount;
    private long maintenanceUnitsCount;
    private long reservedUnitsCount;

    private Instant createdAt;
    private Instant updatedAt;
    private Long createdBy;
}
