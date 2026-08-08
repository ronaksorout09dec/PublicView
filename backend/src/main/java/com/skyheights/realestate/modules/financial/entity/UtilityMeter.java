package com.skyheights.realestate.modules.financial.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "utility_meters", indexes = {
    @Index(name = "idx_meters_property", columnList = "property_id"),
    @Index(name = "idx_meters_unit", columnList = "unit_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UtilityMeter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utility_type_id", nullable = false)
    private UtilityType utilityType;

    @Column(name = "meter_number", nullable = false, unique = true, length = 100)
    private String meterNumber;

    @Column(name = "is_shared")
    @Builder.Default
    private Boolean isShared = false;

    @Column(name = "location")
    private String location;

    @Column(name = "total_units_sharing")
    @Builder.Default
    private Integer totalUnitsSharing = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ratio_config", columnDefinition = "jsonb")
    private String ratioConfig;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "ACTIVE";
}
