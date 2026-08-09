package com.skyheights.realestate.modules.crm.dto;

import com.skyheights.realestate.modules.crm.enums.LeadSource;
import com.skyheights.realestate.modules.crm.enums.LeadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmLeadResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long propertyId;
    private String propertyName;
    private Long unitId;
    private String unitNumber;
    private String interestedUnitType;
    private String customerName;
    private String phone;
    private String email;
    private LeadSource source;
    private LeadStatus status;
    private String priority;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String configuration;
    private String timeline;
    private String purpose;
    private Long assignedToStaffId;
    private String assignedToStaffName;
    private String notes;
    private String conversationSummary;
    private String lostReason;
    private Instant nextFollowupAt;
    private BigDecimal aiScore;
    private Instant createdAt;
    private Instant updatedAt;
    private long visitsCount;
}
