package com.skyheights.realestate.modules.communication.dto;

import com.skyheights.realestate.modules.communication.enums.AutomationTrigger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationRuleResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private String name;
    private String code;
    private String description;
    private AutomationTrigger triggerEvent;
    private Map<String, Object> conditions;
    private Long templateId;
    private String templateCode;
    private Boolean isActive;
    private Integer cooldownHours;
    private Instant lastTriggeredAt;
    private Long executionCount;
    private Instant createdAt;
}
