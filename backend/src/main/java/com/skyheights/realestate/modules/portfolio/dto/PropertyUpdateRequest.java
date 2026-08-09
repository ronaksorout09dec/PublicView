package com.skyheights.realestate.modules.portfolio.dto;

import com.skyheights.realestate.modules.portfolio.enums.PropertyStatus;
import com.skyheights.realestate.modules.portfolio.enums.PropertyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyUpdateRequest {

    @Size(max = 255)
    private String name;

    private PropertyType type;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 20)
    private String pincode;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Min(0)
    private Integer totalFloors;

    @Min(0)
    private Integer totalUnits;

    private Integer yearBuilt;

    private Long managerId;

    private PropertyStatus status;

    private String description;

    private Set<Long> amenityIds;

    private String thumbnailS3Key;
}
