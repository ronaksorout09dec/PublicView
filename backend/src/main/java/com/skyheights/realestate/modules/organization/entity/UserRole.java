package com.skyheights.realestate.modules.organization.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_roles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_role_org", columnNames = {"user_id","role_id","org_id"})
}, indexes = {
    @Index(name = "idx_user_roles_user", columnList = "user_id"),
    @Index(name = "idx_user_roles_role", columnList = "role_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "assigned_at")
    @CreationTimestamp
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private Long assignedBy;
}
