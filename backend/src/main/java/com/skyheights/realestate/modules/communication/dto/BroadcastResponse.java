package com.skyheights.realestate.modules.communication.dto;

import com.skyheights.realestate.modules.communication.enums.BroadcastPriority;
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
public class BroadcastResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long propertyId;
    private String propertyName;
    private Long unitId;
    private String unitNumber;
    private String title;
    private String message;
    private BroadcastPriority priority;
    private String category;
    private Long createdByUserId;
    private String createdByName;
    private Instant expiresAt;
    private Boolean isActive;
    private String attachmentS3Key;
    private String attachmentPresignedUrl;
    private Boolean actionRequired;
    private String actionLabel;
    private Boolean sendPush;
    private Boolean sendSms;
    private Boolean sendWhatsapp;
    private Boolean sendEmail;
    private Instant createdAt;

    private long totalRecipients;
    private long deliveredCount;
    private long readCount;

    private List<RecipientStatus> recipients;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecipientStatus {
        private Long id;
        private Long recipientUserId;
        private String recipientName;
        private String recipientContact;
        private String status;
        private Instant readAt;
    }
}
