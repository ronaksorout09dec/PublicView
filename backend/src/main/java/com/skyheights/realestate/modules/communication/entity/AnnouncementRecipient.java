package com.skyheights.realestate.modules.communication.entity;

import com.skyheights.realestate.modules.organization.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "announcement_recipients", uniqueConstraints = {
    @UniqueConstraint(name = "uk_announcement_recipient", columnNames = {"announcement_id","recipient_user_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnouncementRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private BroadcastAnnouncement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private AppUser recipientUser;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "SENT";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivered_via", columnDefinition = "jsonb")
    private String deliveredVia;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
