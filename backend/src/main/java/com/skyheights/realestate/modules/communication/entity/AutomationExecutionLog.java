package com.skyheights.realestate.modules.communication.entity;

import com.skyheights.realestate.modules.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "automation_execution_logs", indexes = {
    @Index(name = "idx_auto_exec_rule", columnList = "rule_id, triggered_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AutomationExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    @Builder.Default
    private String uuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private AutomationRule rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    private String contextJson;

    @Column(name = "affected_recipients_count")
    @Builder.Default
    private Integer affectedRecipientsCount = 0;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @PrePersist
    public void prePersist() {
        if (triggeredAt == null) triggeredAt = Instant.now();
        if (uuid == null) uuid = UUID.randomUUID().toString();
    }
}
