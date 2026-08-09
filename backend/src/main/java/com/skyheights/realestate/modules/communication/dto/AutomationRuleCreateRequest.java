package com.skyheights.realestate.modules.communication.dto;

import com.skyheights.realestate.modules.communication.enums.AutomationTrigger;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationRuleCreateRequest {

    @NotBlank(message = "Name required")
    private String name;

    @NotBlank(message = "Code required e.g AUTO_RENT_DUE_T_MINUS_3")
    private String code;

    private String description;

    @NotNull(message = "Trigger event required")
    private AutomationTrigger triggerEvent;

    private Map<String, Object> conditions; // {"property_id_in":[],"unit_status":"occupied"}

    private Long templateId;

    private Boolean isActive;
    private Integer cooldownHours;
}
