package com.skyheights.realestate.modules.communication.dto;

import com.skyheights.realestate.modules.communication.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendNotificationRequest {

    private Long templateId; // optional, if provided use template rendering
    private String templateCode; // alternative to templateId

    @NotNull(message = "Channel required")
    private NotificationChannel channel;

    @NotBlank(message = "Recipient contact required - phone/email")
    private String recipientContact;

    private Long recipientUserId;
    private String recipientType; // TENANT, VENDOR, STAFF, LEAD, USER

    // For direct send without template
    private String subject;
    private String body;

    // Variables for template rendering {{tenant_name}} etc
    private Map<String, String> variables;

    private String relatedEntityType; // INVOICE, LEASE, TICKET
    private Long relatedEntityId;
}
