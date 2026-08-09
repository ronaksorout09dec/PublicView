package com.skyheights.realestate.modules.crm.dto;

import com.skyheights.realestate.modules.crm.enums.LeadSource;
import jakarta.validation.constraints.NotBlank;
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
public class CrmLeadCreateRequest {

    private Long propertyId;
    private Long unitId;
    private String interestedUnitType;

    @NotBlank(message = "Customer name required")
    private String customerName;

    @NotBlank(message = "Phone required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
    private String phone;

    private String email;

    private LeadSource source;

    private String priority; // HIGH, MEDIUM, LOW

    private BigDecimal budgetMin;
    private BigDecimal budgetMax;

    private String configuration; // 1BHK, 2BHK etc
    private String timeline;
    private String purpose;

    private Long assignedToStaffId;

    private String notes;

    private String conversationSummary;

    private Instant nextFollowupAt;

    private BigDecimal aiScore;
}
