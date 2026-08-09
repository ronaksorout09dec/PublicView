package com.skyheights.realestate.modules.crm.dto;

import com.skyheights.realestate.modules.crm.enums.LeadSource;
import com.skyheights.realestate.modules.crm.enums.LeadStatus;
import jakarta.validation.constraints.Pattern;
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
public class CrmLeadUpdateRequest {

    private Long propertyId;
    private Long unitId;
    private String interestedUnitType;

    private String customerName;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
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
    private String notes;
    private String conversationSummary;
    private String lostReason;
    private Instant nextFollowupAt;
    private BigDecimal aiScore;
}
