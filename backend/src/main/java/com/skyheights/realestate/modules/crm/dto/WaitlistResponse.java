package com.skyheights.realestate.modules.crm.dto;

import com.skyheights.realestate.modules.crm.enums.WaitlistStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long propertyId;
    private String propertyName;
    private String unitType;
    private Long leadId;
    private String leadCustomerName;
    private String leadPhone;
    private Integer position;
    private WaitlistStatus status;
    private Integer priorityScore;
    private LocalDate desiredMoveIn;
    private Instant createdAt;
}
