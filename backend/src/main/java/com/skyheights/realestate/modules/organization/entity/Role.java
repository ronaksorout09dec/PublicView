package com.skyheights.realestate.modules.organization.entity;

import com.skyheights.realestate.modules.organization.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "roles", uniqueConstraints = {
    @UniqueConstraint(name = "uk_org_role_name", columnNames = {"org_id", "name"})
}, indexes = {
    @Index(name = "idx_roles_org", columnList = "org_id"),
    @Index(name = "idx_roles_name", columnList = "name")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false, length = 36)
    @Builder.Default
    private String uuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private Organization organization;

    @Column(name = "org_id", insertable = false, updatable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 100)
    private RoleName name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "hierarchy_level", nullable = false)
    private Integer hierarchyLevel;

    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"),
        foreignKey = @ForeignKey(name = "fk_role_perm_role"),
        inverseForeignKey = @ForeignKey(name = "fk_role_perm_perm")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
