package com.skyheights.realestate.modules.communication.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.communication.enums.NotificationChannel;
import com.skyheights.realestate.modules.communication.enums.NotificationStatus;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_notif_org", columnList = "org_id, created_at"),
    @Index(name = "idx_notif_status", columnList = "status"),
    @Index(name = "idx_notif_recipient", columnList = "recipient_contact"),
    @Index(name = "idx_notif_related", columnList = "related_entity_type, related_entity_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private NotificationTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "recipient_type", nullable = false, length = 30)
    private String recipientType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private AppUser recipient;

    @Column(name = "recipient_contact", nullable = false)
    private String recipientContact;

    @Column(name = "subject_rendered", length = 500)
    private String subjectRendered;

    @Column(name = "body_rendered", columnDefinition = "TEXT")
    private String bodyRendered;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.QUEUED;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "related_entity_type", length = 100)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
}
