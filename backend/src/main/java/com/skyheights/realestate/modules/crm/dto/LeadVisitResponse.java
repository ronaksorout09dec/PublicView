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
public class LeadVisitResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long leadId;
    private String leadCustomerName;
    private Long propertyId;
    private String propertyName;
    private Long unitId;
    private String unitNumber;
    private Instant scheduledAt;
    private Instant visitedAt;
    private VisitStatus status;
    private String notes;
    private String feedback;
    private Long staffId;
    private String staffName;
    private Instant createdAt;
}
