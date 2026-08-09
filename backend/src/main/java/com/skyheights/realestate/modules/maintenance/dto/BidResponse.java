package com.skyheights.realestate.modules.maintenance.dto;

import com.skyheights.realestate.modules.maintenance.enums.BidStatus;
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
public class BidResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long ticketId;
    private String ticketTitle;
    private Long vendorId;
    private String vendorCompanyName;
    private String vendorName;
    private BigDecimal bidAmount;
    private Integer estimatedDays;
    private String proposal;
    private BidStatus status;
    private Instant submittedAt;
    private Instant approvedAt;
    private String rejectionReason;
    private Boolean includesMaterial;
    private Integer warrantyDays;
    private Instant createdAt;
}
