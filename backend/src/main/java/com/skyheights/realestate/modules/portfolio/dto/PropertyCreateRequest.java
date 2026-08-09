package com.skyheights.realestate.modules.portfolio.dto;

import com.skyheights.realestate.modules.portfolio.enums.PropertyType;
import jakarta.validation.constraints.*;
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
public class PropertyCreateRequest {

    @NotBlank(message = "Property name is required")
    @Size(max = 255, message = "Name max 255 chars")
    private String name;

    @NotNull(message = "Property type is required")
    private PropertyType type;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 20)
    private String pincode;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Min(value = 0, message = "Total floors cannot be negative")
    private Integer totalFloors;

    @Min(value = 0, message = "Total units cannot be negative")
    private Integer totalUnits;

    private Integer yearBuilt;

    private Long managerId; // Must belong to same org

    private String description;

    private Set<Long> amenityIds; // Existing amenity IDs

    private String thumbnailS3Key; // For Phase 3, direct S3 key, upload via S3Service separate endpoint
}
