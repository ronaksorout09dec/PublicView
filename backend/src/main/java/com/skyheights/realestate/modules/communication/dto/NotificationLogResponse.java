package com.skyheights.realestate.modules.communication.dto;

import com.skyheights.realestate.modules.communication.enums.NotificationChannel;
import com.skyheights.realestate.modules.communication.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLogResponse {

    private Long id;
    private String uuid;
    private Long orgId;
    private Long templateId;
    private String templateCode;
    private NotificationChannel channel;
    private String recipientType;
    private Long recipientId;
    private String recipientContact;
    private String subjectRendered;
    private String bodyRendered;
    private NotificationStatus status;
    private String providerMessageId;
    private Instant sentAt;
    private Instant deliveredAt;
    private String failureReason;
    private String relatedEntityType;
    private Long relatedEntityId;
    private Integer retryCount;
    private Instant nextRetryAt;
    private Instant createdAt;
}
