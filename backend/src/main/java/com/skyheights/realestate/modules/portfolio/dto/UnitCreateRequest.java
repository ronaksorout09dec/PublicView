package com.skyheights.realestate.modules.portfolio.dto;

import com.skyheights.realestate.modules.portfolio.enums.UnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
public class UnitCreateRequest {

    @NotNull(message = "Property ID required")
    private Long propertyId;

    @NotBlank(message = "Unit number required")
    private String unitNumber;

    private Integer floor;

    @NotNull(message = "Unit type required")
    private UnitType type;

    @Positive(message = "Size must be positive")
    private BigDecimal sizeSqft;

    private Integer bedrooms;
    private Integer bathrooms;

    @NotNull(message = "Rent amount required")
    @Positive(message = "Rent must be positive")
    private BigDecimal rentAmount;

    @PositiveOrZero(message = "Deposit must be >=0")
    private BigDecimal depositAmount;

    private String description;

    private Set<Long> amenityIds;
}
