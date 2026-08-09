package com.skyheights.realestate.modules.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityResponse {
    private Long id;
    private String uuid;
    private String name;
    private String category;
    private String icon;
    private String description;
}
