package com.skyheights.realestate.modules.tenant.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.organization.entity.Role;
import com.skyheights.realestate.modules.organization.entity.UserRole;
import com.skyheights.realestate.modules.organization.enums.RoleName;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.organization.repository.RoleRepository;
import com.skyheights.realestate.modules.organization.repository.UserRoleRepository;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.portfolio.repository.PropertyRepository;
import com.skyheights.realestate.modules.portfolio.repository.UnitRepository;
import com.skyheights.realestate.modules.tenant.dto.TenantCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.TenantResponse;
import com.skyheights.realestate.modules.tenant.dto.TenantUpdateRequest;
import com.skyheights.realestate.modules.tenant.entity.TenantProfile;
import com.skyheights.realestate.modules.tenant.enums.TenantStatus;
import com.skyheights.realestate.modules.tenant.repository.KycDocumentRepository;
import com.skyheights.realestate.modules.tenant.repository.LeaseAgreementRepository;
import com.skyheights.realestate.modules.tenant.repository.TenantProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantProfileRepository tenantRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final KycDocumentRepository kycRepository;
    private final LeaseAgreementRepository leaseRepository;
    private final S3Service s3Service;

    @Transactional
    public TenantResponse createTenant(Long orgId, TenantCreateRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Property property = null;
        if (request.getPropertyId() != null) {
            property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        }

        Unit unit = null;
        if (request.getUnitId() != null) {
            unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUnitId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            if (property != null && !unit.getProperty().getId().equals(property.getId())) {
                throw new RuntimeException("Unit does not belong to property");
            }
            // Edge: unit must be VACANT or RESERVED to assign tenant
            if (unit.getStatus() != com.skyheights.realestate.modules.portfolio.enums.UnitStatus.VACANT &&
                    unit.getStatus() != com.skyheights.realestate.modules.portfolio.enums.UnitStatus.RESERVED) {
                throw new RuntimeException("Unit must be VACANT or RESERVED to onboard tenant, current: " + unit.getStatus());
            }
        }

        AppUser user;
        if (request.getUserId() != null) {
            user = appUserRepository.findByIdAndIsDeletedFalse(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Long userOrgId = user.getOrganization() != null ? user.getOrganization().getId() : user.getOrgId();
            if (!userOrgId.equals(orgId)) {
                throw new RuntimeException("User must belong to same organization");
            }
        } else {
            // Create new AppUser for tenant
            if (request.getEmail() == null || request.getFullName() == null) {
                throw new RuntimeException("For new tenant user, fullName and email required");
            }
            if (appUserRepository.existsByEmailIgnoreCaseAndOrgId(request.getEmail(), orgId)) {
                throw new RuntimeException("Email already exists in organization");
            }
            String rawPassword = request.getPassword() != null ? request.getPassword() : "Tenant123!";
            user = AppUser.builder()
                    .email(request.getEmail().toLowerCase())
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .organization(org)
                    .build();
            user = appUserRepository.save(user);

            // Assign TENANT role
            Role tenantRole = roleRepository.findByNameAndOrgIdIsNull(RoleName.TENANT)
                    .orElseThrow(() -> new RuntimeException("TENANT role not found"));
            UserRole ur = UserRole.builder()
                    .user(user)
                    .role(tenantRole)
                    .organization(org)
                    .build();
            userRoleRepository.save(ur);
            log.info("Created new AppUser {} for tenant org {}", user.getEmail(), orgId);
        }

        TenantProfile tenant = TenantProfile.builder()
                .organization(org)
                .user(user)
                .property(property)
                .unit(unit)
                .tenancyType(request.getTenancyType() != null ? request.getTenancyType() : "PRIMARY")
                .employerName(request.getEmployerName())
                .occupation(request.getOccupation())
                .monthlyIncome(request.getMonthlyIncome())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .moveInDate(request.getMoveInDate())
                .expectedMoveOutDate(request.getExpectedMoveOutDate())
                .status(TenantStatus.PROSPECT)
                .notes(request.getNotes())
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Created tenant profile {} for org {}", tenant.getId(), orgId);
        return toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public Page<TenantResponse> searchTenants(Long orgId, Long propertyId, Long unitId, TenantStatus status, String search, Pageable pageable) {
        Page<TenantProfile> page = tenantRepository.search(orgId, propertyId, unitId, status, search, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenant(Long orgId, Long id) {
        TenantProfile tenant = tenantRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse updateTenant(Long orgId, Long id, TenantUpdateRequest request) {
        TenantProfile tenant = tenantRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (request.getPropertyId() != null) {
            Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
            tenant.setProperty(property);
        }
        if (request.getUnitId() != null) {
            Unit unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUnitId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            tenant.setUnit(unit);
        }
        if (request.getTenancyType() != null) tenant.setTenancyType(request.getTenancyType());
        if (request.getEmployerName() != null) tenant.setEmployerName(request.getEmployerName());
        if (request.getOccupation() != null) tenant.setOccupation(request.getOccupation());
        if (request.getMonthlyIncome() != null) tenant.setMonthlyIncome(request.getMonthlyIncome());
        if (request.getEmergencyContactName() != null) tenant.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) tenant.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getMoveInDate() != null) tenant.setMoveInDate(request.getMoveInDate());
        if (request.getExpectedMoveOutDate() != null) tenant.setExpectedMoveOutDate(request.getExpectedMoveOutDate());
        if (request.getActualMoveOutDate() != null) tenant.setActualMoveOutDate(request.getActualMoveOutDate());
        if (request.getStatus() != null) {
            validateStatusTransition(tenant.getStatus(), request.getStatus());
            tenant.setStatus(request.getStatus());
            // If MOVED_OUT, set actualMoveOutDate if not set
            if (request.getStatus() == TenantStatus.MOVED_OUT && tenant.getActualMoveOutDate() == null) {
                tenant.setActualMoveOutDate(LocalDate.now());
            }
        }
        if (request.getNotes() != null) tenant.setNotes(request.getNotes());

        tenant = tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional
    public void deleteTenant(Long orgId, Long id) {
        TenantProfile tenant = tenantRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        // Edge: cannot delete if has ACTIVE lease
        boolean hasActiveLease = leaseRepository.findByTenantIdAndIsDeletedFalse(id).stream()
                .anyMatch(l -> l.getStatus() == com.skyheights.realestate.modules.tenant.enums.LeaseStatus.ACTIVE);
        if (hasActiveLease) {
            throw new RuntimeException("Cannot delete tenant with ACTIVE lease. Terminate lease first.");
        }

        tenant.setIsDeleted(true);
        tenantRepository.save(tenant);
        log.info("Soft deleted tenant {} org {}", id, orgId);
    }

    private void validateStatusTransition(TenantStatus current, TenantStatus target) {
        if (current == target) return;
        // PROSPECT -> ACTIVE, PENDING_KYC, BLACKLISTED
        // PENDING_KYC -> ACTIVE, BLACKLISTED
        // ACTIVE -> NOTICE_PERIOD, MOVED_OUT, BLACKLISTED
        // NOTICE_PERIOD -> MOVED_OUT, ACTIVE (if notice withdrawn)
        // MOVED_OUT terminal, BLACKLISTED terminal but can be reactivated to PROSPECT manually?
        switch (current) {
            case PROSPECT:
                if (target != TenantStatus.ACTIVE && target != TenantStatus.PENDING_KYC && target != TenantStatus.BLACKLISTED && target != TenantStatus.MOVED_OUT) {
                    throw new RuntimeException("Invalid PROSPECT -> " + target);
                }
                break;
            case PENDING_KYC:
                if (target != TenantStatus.ACTIVE && target != TenantStatus.BLACKLISTED) {
                    throw new RuntimeException("Invalid PENDING_KYC -> " + target);
                }
                break;
            case ACTIVE:
                if (target != TenantStatus.NOTICE_PERIOD && target != TenantStatus.MOVED_OUT && target != TenantStatus.BLACKLISTED) {
                    throw new RuntimeException("Invalid ACTIVE -> " + target);
                }
                break;
            case NOTICE_PERIOD:
                if (target != TenantStatus.MOVED_OUT && target != TenantStatus.ACTIVE) {
                    throw new RuntimeException("Invalid NOTICE_PERIOD -> " + target);
                }
                break;
            case MOVED_OUT:
                throw new RuntimeException("MOVED_OUT is terminal, cannot transition to " + target);
            case BLACKLISTED:
                if (target != TenantStatus.PROSPECT) {
                    throw new RuntimeException("BLACKLISTED can only go to PROSPECT for re-evaluation");
                }
                break;
        }
    }

    private TenantResponse toResponse(TenantProfile t) {
        long totalKyc = kycRepository.findByTenantIdAndIsDeletedFalse(t.getId()).size();
        long verifiedKyc = kycRepository.findByTenantIdAndVerificationStatusAndIsDeletedFalse(t.getId(), com.skyheights.realestate.modules.tenant.enums.KycStatus.VERIFIED).size();

        var activeLease = leaseRepository.findByTenantIdAndIsDeletedFalse(t.getId()).stream()
                .filter(l -> l.getStatus() == com.skyheights.realestate.modules.tenant.enums.LeaseStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        return TenantResponse.builder()
                .id(t.getId())
                .uuid(t.getUuid())
                .orgId(t.getOrganization() != null ? t.getOrganization().getId() : null)
                .userId(t.getUser() != null ? t.getUser().getId() : null)
                .fullName(t.getUser() != null ? t.getUser().getFullName() : null)
                .email(t.getUser() != null ? t.getUser().getEmail() : null)
                .phone(t.getUser() != null ? t.getUser().getPhone() : null)
                .propertyId(t.getProperty() != null ? t.getProperty().getId() : null)
                .propertyName(t.getProperty() != null ? t.getProperty().getName() : null)
                .unitId(t.getUnit() != null ? t.getUnit().getId() : null)
                .unitNumber(t.getUnit() != null ? t.getUnit().getUnitNumber() : null)
                .tenancyType(t.getTenancyType())
                .employerName(t.getEmployerName())
                .occupation(t.getOccupation())
                .monthlyIncome(t.getMonthlyIncome())
                .emergencyContactName(t.getEmergencyContactName())
                .emergencyContactPhone(t.getEmergencyContactPhone())
                .moveInDate(t.getMoveInDate())
                .expectedMoveOutDate(t.getExpectedMoveOutDate())
                .actualMoveOutDate(t.getActualMoveOutDate())
                .status(t.getStatus())
                .notes(t.getNotes())
                .totalKycDocs(totalKyc)
                .verifiedKycDocs(verifiedKyc)
                .kycComplete(totalKyc > 0 && totalKyc == verifiedKyc)
                .activeLeaseId(activeLease != null ? activeLease.getId() : null)
                .activeLeaseNumber(activeLease != null ? activeLease.getLeaseNumber() : null)
                .activeLeaseEndDate(activeLease != null ? activeLease.getEndDate() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
