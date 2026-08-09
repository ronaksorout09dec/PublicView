package com.skyheights.realestate.modules.portfolio.service;

import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.portfolio.dto.*;
import com.skyheights.realestate.modules.portfolio.entity.Amenity;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.portfolio.enums.UnitStatus;
import com.skyheights.realestate.modules.portfolio.repository.AmenityRepository;
import com.skyheights.realestate.modules.portfolio.repository.PropertyRepository;
import com.skyheights.realestate.modules.portfolio.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
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
public class UnitService {

    private final UnitRepository unitRepository;
    private final PropertyRepository propertyRepository;
    private final OrganizationRepository organizationRepository;
    private final AmenityRepository amenityRepository;
    @Lazy // avoid circular if WaitlistService ever needs UnitService
    private final com.skyheights.realestate.modules.crm.service.WaitlistService waitlistService;

    @Transactional
    @CacheEvict(value = {"units","properties"}, allEntries = true)
    public UnitResponse createUnit(Long orgId, UnitCreateRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found in your org"));

        if (unitRepository.existsByPropertyIdAndUnitNumberAndIsDeletedFalse(request.getPropertyId(), request.getUnitNumber())) {
            throw new RuntimeException("Unit number already exists in property " + request.getPropertyId());
        }

        // Edge: check totalUnits vs current count
        if (property.getTotalUnits() != null) {
            long currentCount = unitRepository.countByPropertyIdAndIsDeletedFalse(property.getId());
            if (currentCount >= property.getTotalUnits()) {
                log.warn("Property {} totalUnits {} exceeded, current {}", property.getId(), property.getTotalUnits(), currentCount);
                // Allow but warn - manager may have increased capacity without updating property total
            }
        }

        Unit unit = Unit.builder()
                .organization(org)
                .property(property)
                .unitNumber(request.getUnitNumber())
                .floor(request.getFloor())
                .type(request.getType())
                .sizeSqft(request.getSizeSqft())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .rentAmount(request.getRentAmount())
                .depositAmount(request.getDepositAmount())
                .description(request.getDescription())
                .status(UnitStatus.VACANT)
                .build();

        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            unit.setAmenities(amenities);
        }

        unit = unitRepository.save(unit);
        log.info("Created unit {} in property {} org {}", unit.getUnitNumber(), property.getId(), orgId);
        return toResponse(unit);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "units", key = "#orgId + '_' + #propertyId + '_' + #status + '_' + #type + '_' + #search + '_' + #pageable.pageNumber")
    public Page<UnitResponse> searchUnits(Long orgId, Long propertyId, com.skyheights.realestate.modules.portfolio.enums.UnitStatus status,
                                          com.skyheights.realestate.modules.portfolio.enums.UnitType type, String search, Pageable pageable) {
        Page<Unit> page = unitRepository.search(orgId, propertyId, status, type, search, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UnitResponse getUnit(Long orgId, Long id) {
        Unit unit = unitRepository.findByIdWithAmenities(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        return toResponse(unit);
    }

    @Transactional
    @CacheEvict(value = {"units","properties"}, allEntries = true)
    public UnitResponse updateUnit(Long orgId, Long id, UnitUpdateRequest request) {
        Unit unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));

        if (request.getUnitNumber() != null && !request.getUnitNumber().equals(unit.getUnitNumber())) {
            if (unitRepository.existsByPropertyIdAndUnitNumberAndIdNotAndIsDeletedFalse(unit.getProperty().getId(), request.getUnitNumber(), id)) {
                throw new RuntimeException("Unit number already exists in property");
            }
            unit.setUnitNumber(request.getUnitNumber());
        }
        if (request.getFloor() != null) unit.setFloor(request.getFloor());
        if (request.getType() != null) unit.setType(request.getType());
        if (request.getSizeSqft() != null) unit.setSizeSqft(request.getSizeSqft());
        if (request.getBedrooms() != null) unit.setBedrooms(request.getBedrooms());
        if (request.getBathrooms() != null) unit.setBathrooms(request.getBathrooms());
        if (request.getRentAmount() != null) unit.setRentAmount(request.getRentAmount());
        if (request.getDepositAmount() != null) unit.setDepositAmount(request.getDepositAmount());
        if (request.getDescription() != null) unit.setDescription(request.getDescription());

        // Status transition logic
        if (request.getStatus() != null) {
            UnitStatus oldStatus = unit.getStatus();
            validateStatusTransition(oldStatus, request.getStatus(), unit);
            unit.setStatus(request.getStatus());
            // Edge: if marking VACANT, clear tenant/lease
            if (request.getStatus() == UnitStatus.VACANT) {
                unit.setCurrentTenantId(null);
                unit.setCurrentLeaseId(null);

                // Edge: trigger waitlist auto-offer
                try {
                    var nextInLine = waitlistService.getNextInLineForProperty(unit.getProperty().getId(), unit.getType().name());
                    if (nextInLine != null) {
                        log.info("🔔 Unit {} (type {}) became VACANT — next in waitlist: lead {} {} phone {} position {} priority {}",
                                unit.getUnitNumber(), unit.getType(), nextInLine.getLeadId(), nextInLine.getLeadCustomerName(),
                                nextInLine.getLeadPhone(), nextInLine.getPosition(), nextInLine.getPriorityScore());
                        // Future Phase 3.5: send notification via Communication domain, auto-create NotificationLog
                        // For now, log + keep waitlist status as WAITING, manager can manually offer via PATCH /waitlist/{id}/status?status=OFFERED
                    } else {
                        log.info("Unit {} became VACANT, no waitlist entries for type {} in property {}", unit.getUnitNumber(), unit.getType(), unit.getProperty().getId());
                    }
                } catch (Exception e) {
                    log.warn("Failed to check waitlist for vacant unit {}: {}", unit.getId(), e.getMessage());
                }
            }
            log.info("Unit {} status {} -> {} org {}", unit.getId(), oldStatus, request.getStatus(), orgId);
        }

        if (request.getAmenityIds() != null) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            unit.setAmenities(amenities);
        }

        unit = unitRepository.save(unit);
        return toResponse(unit);
    }

    @Transactional
    @CacheEvict(value = {"units","properties"}, allEntries = true)
    public void deleteUnit(Long orgId, Long id) {
        Unit unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));

        if (unit.getStatus() == UnitStatus.OCCUPIED) {
            throw new RuntimeException("Cannot delete occupied unit. Move out tenant first.");
        }

        unit.setIsDeleted(true);
        unitRepository.save(unit);
        log.info("Soft deleted unit {} org {}", id, orgId);
    }

    @Transactional(readOnly = true)
    public Page<UnitResponse> getVacantUnits(Long orgId, Long propertyId, Pageable pageable) {
        return unitRepository.findByOrganizationIdAndPropertyIdAndIsDeletedFalse(orgId, propertyId, pageable)
                .map(u -> {
                    if (u.getStatus() == UnitStatus.VACANT) return toResponse(u);
                    return null;
                }); // better to use custom query, but for simplicity filter at DB level below
    }

    public Page<UnitResponse> getVacantUnitsFiltered(Long orgId, Long propertyId, Pageable pageable) {
        Page<Unit> page = unitRepository.search(orgId, propertyId, UnitStatus.VACANT, null, null, pageable);
        return page.map(this::toResponse);
    }

    private void validateStatusTransition(UnitStatus current, UnitStatus target, Unit unit) {
        // Simple state machine
        // VACANT -> RESERVED, OCCUPIED, MAINTENANCE
        // RESERVED -> OCCUPIED, VACANT, CANCELLED?
        // OCCUPIED -> NOTICE_PERIOD, VACANT (after move-out), MAINTENANCE
        // NOTICE_PERIOD -> VACANT
        // MAINTENANCE -> VACANT
        if (current == target) return;

        switch (current) {
            case VACANT:
                if (target != UnitStatus.RESERVED && target != UnitStatus.OCCUPIED && target != UnitStatus.MAINTENANCE && target != UnitStatus.NOT_AVAILABLE) {
                    throw new RuntimeException("Invalid transition from VACANT to " + target);
                }
                break;
            case RESERVED:
                if (target != UnitStatus.OCCUPIED && target != UnitStatus.VACANT) {
                    throw new RuntimeException("Invalid transition from RESERVED to " + target);
                }
                break;
            case OCCUPIED:
                if (target != UnitStatus.NOTICE_PERIOD && target != UnitStatus.VACANT && target != UnitStatus.MAINTENANCE) {
                    throw new RuntimeException("Invalid transition from OCCUPIED to " + target);
                }
                break;
            case NOTICE_PERIOD:
                if (target != UnitStatus.VACANT && target != UnitStatus.OCCUPIED) {
                    throw new RuntimeException("Invalid transition from NOTICE_PERIOD to " + target);
                }
                break;
            case MAINTENANCE:
                if (target != UnitStatus.VACANT) {
                    throw new RuntimeException("Invalid transition from MAINTENANCE to " + target + ". Only VACANT allowed after maintenance");
                }
                break;
            default:
                // Allow for NOT_AVAILABLE, etc
                break;
        }
    }

    private UnitResponse toResponse(Unit u) {
        Set<AmenityResponse> amenityResponses = u.getAmenities() != null ?
                u.getAmenities().stream().map(a -> AmenityResponse.builder()
                        .id(a.getId()).uuid(a.getUuid()).name(a.getName())
                        .category(a.getCategory() != null ? a.getCategory().name() : null)
                        .icon(a.getIcon()).description(a.getDescription()).build())
                        .collect(Collectors.toSet()) : Set.of();

        return UnitResponse.builder()
                .id(u.getId())
                .uuid(u.getUuid())
                .orgId(u.getOrganization() != null ? u.getOrganization().getId() : null)
                .propertyId(u.getProperty() != null ? u.getProperty().getId() : null)
                .propertyName(u.getProperty() != null ? u.getProperty().getName() : null)
                .unitNumber(u.getUnitNumber())
                .floor(u.getFloor())
                .type(u.getType())
                .sizeSqft(u.getSizeSqft())
                .bedrooms(u.getBedrooms())
                .bathrooms(u.getBathrooms())
                .rentAmount(u.getRentAmount())
                .depositAmount(u.getDepositAmount())
                .status(u.getStatus())
                .description(u.getDescription())
                .currentTenantId(u.getCurrentTenantId())
                .currentLeaseId(u.getCurrentLeaseId())
                .amenities(amenityResponses)
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
