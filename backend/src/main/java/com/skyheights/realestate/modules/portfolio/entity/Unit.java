package com.skyheights.realestate.modules.portfolio.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.enums.UnitStatus;
import com.skyheights.realestate.modules.portfolio.enums.UnitType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "units", uniqueConstraints = {
    @UniqueConstraint(name = "uk_property_unit_number", columnNames = {"property_id","unit_number"})
}, indexes = {
    @Index(name = "idx_units_org", columnList = "org_id"),
    @Index(name = "idx_units_property", columnList = "property_id"),
    @Index(name = "idx_units_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Unit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "unit_number", nullable = false, length = 50)
    private String unitNumber;

    @Column(name = "floor")
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private UnitType type;

    @Column(name = "size_sqft", precision = 10, scale = 2)
    private BigDecimal sizeSqft;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "rent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "deposit_amount", precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private UnitStatus status = UnitStatus.VACANT;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Denormalized current occupant for fast vacancy checks — actual source of truth is lease
    @Column(name = "current_tenant_id")
    private Long currentTenantId;

    @Column(name = "current_lease_id")
    private Long currentLeaseId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "unit_amenities",
        joinColumns = @JoinColumn(name = "unit_id"),
        inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();
}
