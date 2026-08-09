package com.skyheights.realestate.modules.tenant.dto;

import com.skyheights.realestate.modules.tenant.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistTemplateResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private ReportType type;
    private String name;
    private String description;
    private String itemsJson;
    private Boolean isActive;
    private Instant createdAt;
}
