package com.skyheights.realestate.modules.portfolio.dto;

import com.skyheights.realestate.modules.portfolio.enums.UnitStatus;
import com.skyheights.realestate.modules.portfolio.enums.UnitType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
public class UnitUpdateRequest {

    @Size(max = 50)
    private String unitNumber;

    private Integer floor;

    private UnitType type;

    @Positive
    private BigDecimal sizeSqft;

    private Integer bedrooms;
    private Integer bathrooms;

    @Positive
    private BigDecimal rentAmount;

    @PositiveOrZero
    private BigDecimal depositAmount;

    private UnitStatus status;

    private String description;

    private Set<Long> amenityIds;
}
