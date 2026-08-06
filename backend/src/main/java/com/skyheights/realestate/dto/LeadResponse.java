package com.skyheights.realestate.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadResponse {
    private Long id;
    private String customerName;
    private String phone;
    private String location;
    private String propertyType;
    private String configuration;
    private String budget;
    private String purpose;
    private String timeline;
    private String conversationSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
