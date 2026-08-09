package com.skyheights.realestate.modules.financial.dto;

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
public class UtilityTypeResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private String name;
    private String unitLabel;
    private BigDecimal defaultRate;
    private Instant createdAt;
}
