package com.skyheights.realestate.modules.crm.dto;

import com.skyheights.realestate.modules.crm.enums.VisitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadVisitUpdateRequest {

    private Instant scheduledAt;
    private Instant visitedAt;
    private VisitStatus status;
    private String notes;
    private String feedback;
    private Long staffId;
}
