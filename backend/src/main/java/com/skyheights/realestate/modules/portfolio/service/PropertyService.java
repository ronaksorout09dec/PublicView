package com.skyheights.realestate.modules.portfolio.service;

import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.portfolio.dto.*;
import com.skyheights.realestate.modules.portfolio.entity.Amenity;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.enums.PropertyStatus;
import com.skyheights.realestate.modules.portfolio.enums.PropertyType;
import com.skyheights.realestate.modules.portfolio.enums.UnitStatus;
import com.skyheights.realestate.modules.portfolio.repository.AmenityRepository;
import com.skyheights.realestate.modules.portfolio.repository.PropertyRepository;
import com.skyheights.realestate.modules.portfolio.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final AmenityRepository amenityRepository;
    private final UnitRepository unitRepository;

    @Transactional
    @CacheEvict(value = "properties", allEntries = true)
    public PropertyResponse createProperty(Long orgId, PropertyCreateRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        // Validate manager belongs to same org if provided
        AppUser manager = null;
        if (request.getManagerId() != null) {
            manager = appUserRepository.findByIdAndIsDeletedFalse(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager user not found"));
            Long managerOrgId = manager.getOrganization() != null ? manager.getOrganization().getId() : manager.getOrgId();
            if (!managerOrgId.equals(orgId)) {
                throw new RuntimeException("Manager must belong to same organization");
            }
        }

        Property property = Property.builder()
                .organization(org)
                .name(request.getName())
                .type(request.getType())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .totalFloors(request.getTotalFloors())
                .totalUnits(request.getTotalUnits())
                .yearBuilt(request.getYearBuilt())
                .manager(manager)
                .description(request.getDescription())
                .thumbnailS3Key(request.getThumbnailS3Key())
                .status(PropertyStatus.ACTIVE)
                .build();

        // Amenities
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            // Validate all belong to org
            amenities.forEach(a -> {
                if (!a.getOrganization().getId().equals(orgId)) {
                    throw new RuntimeException("Amenity " + a.getName() + " does not belong to org");
                }
            });
            property.setAmenities(amenities);
        }

        property = propertyRepository.save(property);
        log.info("Created property {} for org {}", property.getName(), orgId);
        return toResponse(property);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "properties", key = "#orgId + '_' + #city + '_' + #search + '_' + #type + '_' + #status + '_' + #pageable.pageNumber")
    public Page<PropertyResponse> searchProperties(Long orgId, String city, String search, PropertyType type, PropertyStatus status, Pageable pageable) {
        Page<Property> page = propertyRepository.search(orgId, city, search, type, status, pageable);
        return page.map(this::toResponseWithStats);
    }

    @Transactional(readOnly = true)
    public PropertyResponse getProperty(Long orgId, Long id) {
        Property property = propertyRepository.findByIdWithAmenities(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        return toResponseWithStats(property);
    }

    @Transactional
    @CacheEvict(value = "properties", allEntries = true)
    public PropertyResponse updateProperty(Long orgId, Long id, PropertyUpdateRequest request) {
        Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (request.getName() != null) property.setName(request.getName());
        if (request.getType() != null) property.setType(request.getType());
        if (request.getAddress() != null) property.setAddress(request.getAddress());
        if (request.getCity() != null) property.setCity(request.getCity());
        if (request.getState() != null) property.setState(request.getState());
        if (request.getPincode() != null) property.setPincode(request.getPincode());
        if (request.getLatitude() != null) property.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) property.setLongitude(request.getLongitude());
        if (request.getTotalFloors() != null) property.setTotalFloors(request.getTotalFloors());
        if (request.getTotalUnits() != null) property.setTotalUnits(request.getTotalUnits());
        if (request.getYearBuilt() != null) property.setYearBuilt(request.getYearBuilt());
        if (request.getStatus() != null) property.setStatus(request.getStatus());
        if (request.getDescription() != null) property.setDescription(request.getDescription());
        if (request.getThumbnailS3Key() != null) property.setThumbnailS3Key(request.getThumbnailS3Key());

        if (request.getManagerId() != null) {
            AppUser manager = appUserRepository.findByIdAndIsDeletedFalse(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            Long managerOrgId = manager.getOrganization() != null ? manager.getOrganization().getId() : manager.getOrgId();
            if (!managerOrgId.equals(orgId)) throw new RuntimeException("Manager must belong to same org");
            property.setManager(manager);
        }

        if (request.getAmenityIds() != null) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            property.setAmenities(amenities);
        }

        property = propertyRepository.save(property);
        return toResponseWithStats(property);
    }

    @Transactional
    @CacheEvict(value = "properties", allEntries = true)
    public void deleteProperty(Long orgId, Long id) {
        Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        // Edge: cannot delete if has active units occupied
        long occupied = unitRepository.countByPropertyIdAndStatusAndIsDeletedFalse(id, UnitStatus.OCCUPIED);
        if (occupied > 0) {
            throw new RuntimeException("Cannot delete property with " + occupied + " occupied units. Move out tenants first.");
        }

        // Soft delete property + cascade soft delete units? For now only property
        property.setIsDeleted(true);
        propertyRepository.save(property);
        log.info("Soft deleted property {} org {}", id, orgId);
    }

    @Transactional(readOnly = true)
    public PropertyResponse getPropertyStats(Long orgId, Long id) {
        return getProperty(orgId, id); // toResponseWithStats already includes counts
    }

    private PropertyResponse toResponse(Property p) {
        Set<AmenityResponse> amenityResponses = p.getAmenities() != null ?
                p.getAmenities().stream().map(this::toAmenityResponse).collect(Collectors.toSet()) : Set.of();

        return PropertyResponse.builder()
                .id(p.getId())
                .uuid(p.getUuid())
                .orgId(p.getOrganization() != null ? p.getOrganization().getId() : null)
                .name(p.getName())
                .type(p.getType())
                .address(p.getAddress())
                .city(p.getCity())
                .state(p.getState())
                .pincode(p.getPincode())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .totalFloors(p.getTotalFloors())
                .totalUnits(p.getTotalUnits())
                .yearBuilt(p.getYearBuilt())
                .managerId(p.getManager() != null ? p.getManager().getId() : null)
                .managerName(p.getManager() != null ? p.getManager().getFullName() : null)
                .status(p.getStatus())
                .description(p.getDescription())
                .thumbnailS3Key(p.getThumbnailS3Key())
                .amenities(amenityResponses)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .createdBy(p.getCreatedBy())
                .build();
    }

    private PropertyResponse toResponseWithStats(Property p) {
        PropertyResponse resp = toResponse(p);
        long total = unitRepository.countByPropertyIdAndIsDeletedFalse(p.getId());
        resp.setUnitsCount(total);
        resp.setVacantUnitsCount(unitRepository.countByPropertyIdAndStatusAndIsDeletedFalse(p.getId(), UnitStatus.VACANT));
        resp.setOccupiedUnitsCount(unitRepository.countByPropertyIdAndStatusAndIsDeletedFalse(p.getId(), UnitStatus.OCCUPIED));
        resp.setMaintenanceUnitsCount(unitRepository.countByPropertyIdAndStatusAndIsDeletedFalse(p.getId(), UnitStatus.MAINTENANCE));
        resp.setReservedUnitsCount(unitRepository.countByPropertyIdAndStatusAndIsDeletedFalse(p.getId(), UnitStatus.RESERVED));
        return resp;
    }

    private AmenityResponse toAmenityResponse(Amenity a) {
        return AmenityResponse.builder()
                .id(a.getId())
                .uuid(a.getUuid())
                .name(a.getName())
                .category(a.getCategory() != null ? a.getCategory().name() : null)
                .icon(a.getIcon())
                .description(a.getDescription())
                .build();
    }
}
