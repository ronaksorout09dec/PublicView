package com.skyheights.realestate.modules.portfolio.dto;

import com.skyheights.realestate.modules.portfolio.enums.AmenityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityCreateRequest {

    @NotBlank(message = "Amenity name required")
    @Size(max = 150)
    private String name;

    private AmenityCategory category;

    @Size(max = 100)
    private String icon;

    @Size(max = 500)
    private String description;
}
