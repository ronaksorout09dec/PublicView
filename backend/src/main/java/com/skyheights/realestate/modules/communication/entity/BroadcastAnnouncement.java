package com.skyheights.realestate.modules.communication.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.communication.enums.BroadcastPriority;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "broadcast_announcements", indexes = {
    @Index(name = "idx_broadcast_org", columnList = "org_id, is_active"),
    @Index(name = "idx_broadcast_property", columnList = "property_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BroadcastAnnouncement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    @Builder.Default
    private BroadcastPriority priority = BroadcastPriority.MEDIUM;

    @Column(name = "category", length = 50)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdByUser;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "attachment_s3_key", length = 500)
    private String attachmentS3Key;

    @Column(name = "action_required")
    @Builder.Default
    private Boolean actionRequired = false;

    @Column(name = "action_label", length = 100)
    private String actionLabel;

    @Column(name = "send_push")
    @Builder.Default
    private Boolean sendPush = true;

    @Column(name = "send_sms")
    @Builder.Default
    private Boolean sendSms = false;

    @Column(name = "send_whatsapp")
    @Builder.Default
    private Boolean sendWhatsapp = true;

    @Column(name = "send_email")
    @Builder.Default
    private Boolean sendEmail = false;

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AnnouncementRecipient> recipients = new ArrayList<>();
}
