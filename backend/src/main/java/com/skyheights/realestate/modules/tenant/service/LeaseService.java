package com.skyheights.realestate.modules.tenant.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.portfolio.enums.UnitStatus;
import com.skyheights.realestate.modules.portfolio.repository.PropertyRepository;
import com.skyheights.realestate.modules.portfolio.repository.UnitRepository;
import com.skyheights.realestate.modules.tenant.dto.LeaseCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.LeaseResponse;
import com.skyheights.realestate.modules.tenant.dto.LeaseUpdateRequest;
import com.skyheights.realestate.modules.tenant.entity.LeaseAgreement;
import com.skyheights.realestate.modules.tenant.entity.TenantProfile;
import com.skyheights.realestate.modules.tenant.enums.LeaseStatus;
import com.skyheights.realestate.modules.tenant.repository.EsignTrackingRepository;
import com.skyheights.realestate.modules.tenant.repository.LeaseAgreementRepository;
import com.skyheights.realestate.modules.tenant.repository.TenantProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaseService {

    private final LeaseAgreementRepository leaseRepository;
    private final TenantProfileRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final OrganizationRepository organizationRepository;
    private final EsignTrackingRepository esignRepository;
    private final S3Service s3Service;

    // Simple atomic for lease number generation in memory; for prod use DB sequence or Redis INCR
    private static final AtomicLong leaseCounter = new AtomicLong(System.currentTimeMillis() % 100000);

    @Transactional
    public LeaseResponse createLease(Long orgId, LeaseCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        Unit unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUnitId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));

        if (!unit.getProperty().getId().equals(property.getId())) {
            throw new RuntimeException("Unit does not belong to property");
        }

        TenantProfile tenant = tenantRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getTenantId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        // Edge validations
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Lease start date cannot be after end date");
        }
        if (request.getStartDate().isBefore(LocalDate.now().minusDays(30))) {
            log.warn("Lease start date {} is more than 30 days in past, allowing but unusual", request.getStartDate());
        }
        if (ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) < 30) {
            throw new RuntimeException("Lease duration must be at least 30 days");
        }
        if (request.getRentDueDay() != null && (request.getRentDueDay() < 1 || request.getRentDueDay() > 28)) {
            throw new RuntimeException("Rent due day must be between 1 and 28");
        }

        // Unit must be VACANT or RESERVED to create ACTIVE lease
        if (unit.getStatus() != UnitStatus.VACANT && unit.getStatus() != UnitStatus.RESERVED) {
            throw new RuntimeException("Unit must be VACANT or RESERVED to create lease, current: " + unit.getStatus());
        }

        // Check no active lease for unit
        if (leaseRepository.existsByUnitIdAndStatusAndIsDeletedFalse(unit.getId(), LeaseStatus.ACTIVE)) {
            throw new RuntimeException("Unit already has an ACTIVE lease");
        }

        // Check tenant doesn't have another ACTIVE lease (one tenant one active lease per org? Allow multiple? For now allow one)
        boolean tenantHasActive = leaseRepository.findByTenantIdAndIsDeletedFalse(tenant.getId()).stream()
                .anyMatch(l -> l.getStatus() == LeaseStatus.ACTIVE);
        if (tenantHasActive) {
            log.warn("Tenant {} already has ACTIVE lease, creating another - maybe co-tenant or second unit", tenant.getId());
            // Allow for demo, but log
        }

        // Generate unique lease number: LEASE-2026-00001
        String leaseNumber = generateLeaseNumber();

        LeaseAgreement parentLease = null;
        if (request.getParentLeaseId() != null) {
            parentLease = leaseRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getParentLeaseId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent lease not found"));
        }

        LeaseAgreement lease = LeaseAgreement.builder()
                .organization(org)
                .property(property)
                .unit(unit)
                .tenant(tenant)
                .leaseNumber(leaseNumber)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .rentAmount(request.getRentAmount())
                .depositAmount(request.getDepositAmount())
                .rentDueDay(request.getRentDueDay() != null ? request.getRentDueDay() : 5)
                .noticePeriodDays(request.getNoticePeriodDays() != null ? request.getNoticePeriodDays() : 30)
                .lockInPeriodMonths(request.getLockInPeriodMonths() != null ? request.getLockInPeriodMonths() : 6)
                .escalationPercent(request.getEscalationPercent() != null ? request.getEscalationPercent() : java.math.BigDecimal.ZERO)
                .status(LeaseStatus.DRAFT)
                .terms(request.getTerms())
                .leaseVersion(1)
                .parentLease(parentLease)
                .build();

        lease = leaseRepository.save(lease);
        log.info("Created lease {} for unit {} tenant {} org {}", leaseNumber, unit.getId(), tenant.getId(), orgId);
        return toResponse(lease);
    }

    @Transactional(readOnly = true)
    public Page<LeaseResponse> searchLeases(Long orgId, Long propertyId, Long unitId, Long tenantId, LeaseStatus status, String search, Pageable pageable) {
        Page<LeaseAgreement> page = leaseRepository.search(orgId, propertyId, unitId, tenantId, status, search, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LeaseResponse getLease(Long orgId, Long id) {
        LeaseAgreement lease = leaseRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        return toResponse(lease);
    }

    @Transactional
    public LeaseResponse updateLease(Long orgId, Long id, LeaseUpdateRequest request) {
        LeaseAgreement lease = leaseRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        if (request.getStartDate() != null) lease.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) lease.setEndDate(request.getEndDate());
        if (request.getRentAmount() != null) lease.setRentAmount(request.getRentAmount());
        if (request.getDepositAmount() != null) lease.setDepositAmount(request.getDepositAmount());
        if (request.getRentDueDay() != null) {
            if (request.getRentDueDay() < 1 || request.getRentDueDay() > 28) throw new RuntimeException("Rent due day 1-28");
            lease.setRentDueDay(request.getRentDueDay());
        }
        if (request.getNoticePeriodDays() != null) lease.setNoticePeriodDays(request.getNoticePeriodDays());
        if (request.getLockInPeriodMonths() != null) lease.setLockInPeriodMonths(request.getLockInPeriodMonths());
        if (request.getEscalationPercent() != null) lease.setEscalationPercent(request.getEscalationPercent());
        if (request.getTerms() != null) lease.setTerms(request.getTerms());
        if (request.getTerminationReason() != null) lease.setTerminationReason(request.getTerminationReason());

        if (request.getStatus() != null) {
            validateStatusTransition(lease.getStatus(), request.getStatus(), lease);
            LeaseStatus oldStatus = lease.getStatus();
            lease.setStatus(request.getStatus());

            // Side effects on status change
            if (request.getStatus() == LeaseStatus.ACTIVE && oldStatus != LeaseStatus.ACTIVE) {
                // Mark unit OCCUPIED
                Unit unit = lease.getUnit();
                unit.setStatus(UnitStatus.OCCUPIED);
                unit.setCurrentTenantId(lease.getTenant().getId());
                unit.setCurrentLeaseId(lease.getId());
                unitRepository.save(unit);

                // Mark tenant ACTIVE
                TenantProfile tenant = lease.getTenant();
                tenant.setStatus(com.skyheights.realestate.modules.tenant.enums.TenantStatus.ACTIVE);
                tenant.setMoveInDate(lease.getStartDate());
                tenantRepository.save(tenant);
                log.info("Lease {} ACTIVE -> unit {} OCCUPIED, tenant {} ACTIVE", lease.getLeaseNumber(), unit.getId(), tenant.getId());
            } else if ((request.getStatus() == LeaseStatus.TERMINATED || request.getStatus() == LeaseStatus.EXPIRED) && oldStatus == LeaseStatus.ACTIVE) {
                // Mark unit VACANT
                Unit unit = lease.getUnit();
                unit.setStatus(UnitStatus.VACANT);
                unit.setCurrentTenantId(null);
                unit.setCurrentLeaseId(null);
                unitRepository.save(unit);

                // Mark tenant MOVED_OUT? Actually NOTICE_PERIOD -> MOVED_OUT handled in tenant service, but we can set NOTICE_PERIOD
                TenantProfile tenant = lease.getTenant();
                if (request.getStatus() == LeaseStatus.TERMINATED) {
                    tenant.setStatus(com.skyheights.realestate.modules.tenant.enums.TenantStatus.MOVED_OUT);
                    tenant.setActualMoveOutDate(LocalDate.now());
                    tenantRepository.save(tenant);
                }
                log.info("Lease {} {} -> unit {} VACANT", lease.getLeaseNumber(), request.getStatus(), unit.getId());
            }
        }

        if (lease.getStartDate() != null && lease.getEndDate() != null && lease.getStartDate().isAfter(lease.getEndDate())) {
            throw new RuntimeException("Start date cannot be after end date");
        }

        lease = leaseRepository.save(lease);
        return toResponse(lease);
    }

    @Transactional
    public void deleteLease(Long orgId, Long id) {
        LeaseAgreement lease = leaseRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        if (lease.getStatus() == LeaseStatus.ACTIVE) {
            throw new RuntimeException("Cannot delete ACTIVE lease. Terminate first.");
        }
        lease.setIsDeleted(true);
        leaseRepository.save(lease);
        log.info("Soft deleted lease {} org {}", id, orgId);
    }

    @Transactional
    public LeaseResponse renewLease(Long orgId, Long id, LocalDate newStartDate, LocalDate newEndDate) {
        LeaseAgreement oldLease = leaseRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        if (oldLease.getStatus() != LeaseStatus.ACTIVE && oldLease.getStatus() != LeaseStatus.EXPIRED) {
            throw new RuntimeException("Only ACTIVE or EXPIRED leases can be renewed");
        }

        // Create new lease with parent reference
        LeaseCreateRequest req = LeaseCreateRequest.builder()
                .propertyId(oldLease.getProperty().getId())
                .unitId(oldLease.getUnit().getId())
                .tenantId(oldLease.getTenant().getId())
                .startDate(newStartDate != null ? newStartDate : oldLease.getEndDate().plusDays(1))
                .endDate(newEndDate != null ? newEndDate : oldLease.getEndDate().plusYears(1))
                .rentAmount(oldLease.getRentAmount()) // could apply escalation
                .depositAmount(oldLease.getDepositAmount())
                .rentDueDay(oldLease.getRentDueDay())
                .noticePeriodDays(oldLease.getNoticePeriodDays())
                .lockInPeriodMonths(oldLease.getLockInPeriodMonths())
                .escalationPercent(oldLease.getEscalationPercent())
                .terms(oldLease.getTerms())
                .parentLeaseId(oldLease.getId())
                .build();

        // Apply escalation if set
        if (oldLease.getEscalationPercent() != null && oldLease.getEscalationPercent().compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal multiplier = java.math.BigDecimal.ONE.add(oldLease.getEscalationPercent().divide(new java.math.BigDecimal("100")));
            req.setRentAmount(oldLease.getRentAmount().multiply(multiplier));
        }

        LeaseResponse newLease = createLease(orgId, req);

        // Mark old as RENEWED
        oldLease.setStatus(LeaseStatus.RENEWED);
        leaseRepository.save(oldLease);

        log.info("Renewed lease {} -> new lease {}", oldLease.getLeaseNumber(), newLease.getLeaseNumber());
        return newLease;
    }

    private void validateStatusTransition(LeaseStatus current, LeaseStatus target, LeaseAgreement lease) {
        if (current == target) return;
        switch (current) {
            case DRAFT:
                if (target != LeaseStatus.PENDING_SIGNATURE && target != LeaseStatus.CANCELLED) {
                    throw new RuntimeException("DRAFT can only go to PENDING_SIGNATURE or CANCELLED");
                }
                break;
            case PENDING_SIGNATURE:
                if (target != LeaseStatus.ACTIVE && target != LeaseStatus.CANCELLED && target != LeaseStatus.DRAFT) {
                    throw new RuntimeException("PENDING_SIGNATURE can only go to ACTIVE, CANCELLED or DRAFT");
                }
                // Check all esign signed before ACTIVE
                if (target == LeaseStatus.ACTIVE) {
                    long total = esignRepository.countByLeaseIdAndIsDeletedFalse(lease.getId());
                    long signed = esignRepository.countByLeaseIdAndStatusAndIsDeletedFalse(lease.getId(), com.skyheights.realestate.modules.tenant.enums.EsignStatus.SIGNED);
                    if (total > 0 && total != signed) {
                        throw new RuntimeException("Cannot activate lease, not all signatories have signed: " + signed + "/" + total);
                    }
                }
                break;
            case ACTIVE:
                if (target != LeaseStatus.EXPIRED && target != LeaseStatus.TERMINATED && target != LeaseStatus.RENEWED) {
                    throw new RuntimeException("ACTIVE can only go to EXPIRED, TERMINATED, RENEWED");
                }
                break;
            case EXPIRED:
                if (target != LeaseStatus.RENEWED && target != LeaseStatus.TERMINATED) {
                    throw new RuntimeException("EXPIRED can only go to RENEWED or TERMINATED");
                }
                break;
            case TERMINATED:
            case CANCELLED:
            case RENEWED:
                throw new RuntimeException(current + " is terminal, cannot transition to " + target);
        }
    }

    private String generateLeaseNumber() {
        long next = leaseCounter.incrementAndGet();
        int year = LocalDate.now().getYear();
        // Check uniqueness, if exists increment
        String candidate;
        do {
            candidate = String.format("LEASE-%d-%05d", year, next);
            next = leaseCounter.incrementAndGet();
        } while (leaseRepository.findByLeaseNumber(candidate).isPresent());
        // Decrement back to last used for next call? Actually we already incremented, keep counter at next
        leaseCounter.set(next);
        return candidate;
    }

    private LeaseResponse toResponse(LeaseAgreement l) {
        long totalEsign = esignRepository.countByLeaseIdAndIsDeletedFalse(l.getId());
        long signedEsign = esignRepository.countByLeaseIdAndStatusAndIsDeletedFalse(l.getId(), com.skyheights.realestate.modules.tenant.enums.EsignStatus.SIGNED);

        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), l.getEndDate());
        boolean expiring60 = daysUntilExpiry >= 0 && daysUntilExpiry <= 60;
        boolean expiring30 = daysUntilExpiry >= 0 && daysUntilExpiry <= 30;
        boolean expired = daysUntilExpiry < 0;

        String presignedUrl = null;
        try {
            if (l.getFinalPdfS3Key() != null) {
                presignedUrl = s3Service.generatePresignedUrl(l.getFinalPdfS3Key(), Duration.ofMinutes(30));
            }
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL for lease {}", l.getId());
        }

        return LeaseResponse.builder()
                .id(l.getId())
                .uuid(l.getUuid())
                .orgId(l.getOrganization() != null ? l.getOrganization().getId() : null)
                .propertyId(l.getProperty() != null ? l.getProperty().getId() : null)
                .propertyName(l.getProperty() != null ? l.getProperty().getName() : null)
                .unitId(l.getUnit() != null ? l.getUnit().getId() : null)
                .unitNumber(l.getUnit() != null ? l.getUnit().getUnitNumber() : null)
                .tenantId(l.getTenant() != null ? l.getTenant().getId() : null)
                .tenantName(l.getTenant() != null && l.getTenant().getUser() != null ? l.getTenant().getUser().getFullName() : null)
                .tenantEmail(l.getTenant() != null && l.getTenant().getUser() != null ? l.getTenant().getUser().getEmail() : null)
                .leaseNumber(l.getLeaseNumber())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .rentAmount(l.getRentAmount())
                .depositAmount(l.getDepositAmount())
                .rentDueDay(l.getRentDueDay())
                .noticePeriodDays(l.getNoticePeriodDays())
                .lockInPeriodMonths(l.getLockInPeriodMonths())
                .escalationPercent(l.getEscalationPercent())
                .status(l.getStatus())
                .terms(l.getTerms())
                .finalPdfS3Key(l.getFinalPdfS3Key())
                .finalPdfPresignedUrl(presignedUrl)
                .leaseVersion(l.getLeaseVersion())
                .parentLeaseId(l.getParentLease() != null ? l.getParentLease().getId() : null)
                .terminationReason(l.getTerminationReason())
                .esignTotal(totalEsign)
                .esignSigned(signedEsign)
                .allSigned(totalEsign > 0 && totalEsign == signedEsign)
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .daysUntilExpiry(daysUntilExpiry)
                .expiringIn60Days(expiring60)
                .expiringIn30Days(expiring30)
                .expired(expired)
                .build();
    }
}
