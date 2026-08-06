package com.skyheights.realestate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallSummaryResponse {
    private String summary;
    private LeadResponse lead;
    private String structuredJson;
    private boolean success;
    private String message;
}
