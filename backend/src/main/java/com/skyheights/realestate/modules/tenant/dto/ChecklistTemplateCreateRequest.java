package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.ReportType;
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
public class ChecklistTemplateCreateRequest {

    @NotNull(message = "Type required")
    private ReportType type;

    @NotBlank(message = "Name required")
    private String name;

    private String description;

    @NotNull(message = "Items JSON required")
    private String itemsJson; // [{"key":"wall_paint","label":"Wall Paint","type":"CONDITION","required":true}]

    private Boolean isActive;
}
