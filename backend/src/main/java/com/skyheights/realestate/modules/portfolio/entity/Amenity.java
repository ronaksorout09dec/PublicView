package com.skyheights.realestate.modules.portfolio.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.portfolio.enums.AmenityCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "amenities", uniqueConstraints = {
    @UniqueConstraint(name = "uk_org_amenity_name", columnNames = {"org_id","name"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Amenity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 100)
    private AmenityCategory category;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "description", length = 500)
    private String description;
}
