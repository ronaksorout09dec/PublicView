package com.skyheights.realestate.modules.maintenance.dto;

import com.skyheights.realestate.modules.maintenance.enums.VendorSpecialization;
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
public class VendorResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String companyName;
    private VendorSpecialization specialization;
    private Integer yearsExperience;
    private BigDecimal rating;
    private Integer totalJobsCompleted;
    private Boolean isVerified;
    private String status;
    private Instant createdAt;

    private BigDecimal totalPaid;
    private BigDecimal pendingPayout;
}
