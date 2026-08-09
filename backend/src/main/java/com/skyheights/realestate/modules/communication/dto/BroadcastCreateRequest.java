package com.skyheights.realestate.modules.communication.dto;

import com.skyheights.realestate.modules.communication.enums.BroadcastPriority;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastCreateRequest {

    private Long propertyId; // null = org-wide
    private Long unitId;

    @NotBlank(message = "Title required")
    private String title;

    @NotBlank(message = "Message required")
    private String message;

    private BroadcastPriority priority;
    private String category; // WATER, ELECTRICITY, MAINTENANCE, EVENT, SAFETY, GENERAL

    private Instant expiresAt;
    private String attachmentS3Key;

    private Boolean actionRequired;
    private String actionLabel;

    private Boolean sendPush;
    private Boolean sendSms;
    private Boolean sendWhatsapp;
    private Boolean sendEmail;

    // Specific recipients, if null = all tenants in property/org
    private List<Long> recipientUserIds;
}
