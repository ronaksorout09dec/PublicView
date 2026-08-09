package com.skyheights.realestate.modules.communication.dto;

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
public class AutomationLogResponse {

    private Long id;
    private String uuid;
    private Long ruleId;
    private String ruleCode;
    private Long orgId;
    private Instant triggeredAt;
    private String status;
    private Map<String, Object> context;
    private Integer affectedRecipientsCount;
    private String details;
    private String error;
}
