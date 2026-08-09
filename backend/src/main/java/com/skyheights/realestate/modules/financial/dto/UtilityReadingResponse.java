package com.skyheights.realestate.modules.financial.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityReadingResponse {

    private Long id;
    private String uuid;
    private Long meterId;
    private String meterNumber;
    private LocalDate readingDate;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private BigDecimal unitsConsumed;
    private BigDecimal ratePerUnit;
    private BigDecimal amount;
    private Long recordedByUserId;
    private String recordedByName;
    private String photoS3Key;
    private String photoPresignedUrl;
    private String source;
    private Instant createdAt;
}
