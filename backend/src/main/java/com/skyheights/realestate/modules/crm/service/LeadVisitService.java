package com.skyheights.realestate.modules.crm.service;

import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.crm.dto.LeadVisitCreateRequest;
import com.skyheights.realestate.modules.crm.dto.LeadVisitResponse;
import com.skyheights.realestate.modules.crm.dto.LeadVisitUpdateRequest;
import com.skyheights.realestate.modules.crm.entity.CrmLead;
import com.skyheights.realestate.modules.crm.entity.LeadVisit;
import com.skyheights.realestate.modules.crm.enums.VisitStatus;
import com.skyheights.realestate.modules.crm.repository.CrmLeadRepository;
import com.skyheights.realestate.modules.crm.repository.LeadVisitRepository;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.portfolio.repository.PropertyRepository;
import com.skyheights.realestate.modules.portfolio.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadVisitService {

    private final LeadVisitRepository visitRepository;
    private final CrmLeadRepository leadRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public LeadVisitResponse createVisit(Long orgId, LeadVisitCreateRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        CrmLead lead = leadRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getLeadId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        Unit unit = null;
        if (request.getUnitId() != null) {
            unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUnitId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            if (!unit.getProperty().getId().equals(property.getId())) {
                throw new RuntimeException("Unit does not belong to property");
            }
        }

        AppUser staff = null;
        if (request.getStaffId() != null) {
            staff = appUserRepository.findByIdAndIsDeletedFalse(request.getStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
            Long staffOrgId = staff.getOrganization() != null ? staff.getOrganization().getId() : staff.getOrgId();
            if (!staffOrgId.equals(orgId)) throw new RuntimeException("Staff must belong to same org");
        }

        // Edge: scheduledAt must be in future (validated via @Future), but also check not too far? Allow 1 year max
        if (request.getScheduledAt().isAfter(Instant.now().plusSeconds(365L * 24 * 3600))) {
            throw new RuntimeException("Cannot schedule visit more than 1 year ahead");
        }

        LeadVisit visit = LeadVisit.builder()
                .organization(org)
                .lead(lead)
                .property(property)
                .unit(unit)
                .scheduledAt(request.getScheduledAt())
                .notes(request.getNotes())
                .staff(staff)
                .status(VisitStatus.SCHEDULED)
                .build();

        visit = visitRepository.save(visit);

        // Update lead status to VISIT_SCHEDULED if currently NEW/CONTACTED
        if (lead.getStatus() == com.skyheights.realestate.modules.crm.enums.LeadStatus.NEW ||
                lead.getStatus() == com.skyheights.realestate.modules.crm.enums.LeadStatus.CONTACTED) {
            lead.setStatus(com.skyheights.realestate.modules.crm.enums.LeadStatus.VISIT_SCHEDULED);
            leadRepository.save(lead);
        }

        log.info("Created visit {} for lead {} org {}", visit.getId(), lead.getId(), orgId);
        return toResponse(visit);
    }

    @Transactional(readOnly = true)
    public Page<LeadVisitResponse> searchVisits(Long orgId, Long leadId, Long propertyId, VisitStatus status, Long staffId, Pageable pageable) {
        Page<LeadVisit> page = visitRepository.search(orgId, leadId, propertyId, status, staffId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LeadVisitResponse getVisit(Long orgId, Long id) {
        LeadVisit visit = visitRepository.findById(id)
                .filter(v -> v.getOrganization().getId().equals(orgId) && !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        return toResponse(visit);
    }

    @Transactional
    public LeadVisitResponse updateVisit(Long orgId, Long id, LeadVisitUpdateRequest request) {
        LeadVisit visit = visitRepository.findById(id)
                .filter(v -> v.getOrganization().getId().equals(orgId) && !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));

        if (request.getScheduledAt() != null) visit.setScheduledAt(request.getScheduledAt());
        if (request.getVisitedAt() != null) visit.setVisitedAt(request.getVisitedAt());
        if (request.getStatus() != null) {
            validateStatusTransition(visit.getStatus(), request.getStatus());
            visit.setStatus(request.getStatus());
            // If completed, set visitedAt if not set
            if (request.getStatus() == VisitStatus.COMPLETED && visit.getVisitedAt() == null) {
                visit.setVisitedAt(Instant.now());
                // Update lead to VISITED
                CrmLead lead = visit.getLead();
                if (lead.getStatus() == com.skyheights.realestate.modules.crm.enums.LeadStatus.VISIT_SCHEDULED) {
                    lead.setStatus(com.skyheights.realestate.modules.crm.enums.LeadStatus.VISITED);
                    leadRepository.save(lead);
                }
            }
        }
        if (request.getNotes() != null) visit.setNotes(request.getNotes());
        if (request.getFeedback() != null) visit.setFeedback(request.getFeedback());
        if (request.getStaffId() != null) {
            AppUser staff = appUserRepository.findByIdAndIsDeletedFalse(request.getStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
            visit.setStaff(staff);
        }

        visit = visitRepository.save(visit);
        return toResponse(visit);
    }

    @Transactional
    public void deleteVisit(Long orgId, Long id) {
        LeadVisit visit = visitRepository.findById(id)
                .filter(v -> v.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found"));
        visit.setIsDeleted(true);
        visitRepository.save(visit);
    }

    private void validateStatusTransition(VisitStatus current, VisitStatus target) {
        if (current == target) return;
        // SCHEDULED -> COMPLETED, CANCELLED, NO_SHOW, RESCHEDULED
        // COMPLETED is terminal, but allow back to SCHEDULED? No
        if (current == VisitStatus.COMPLETED && target != VisitStatus.COMPLETED) {
            throw new RuntimeException("Cannot change status from COMPLETED");
        }
        if (current == VisitStatus.CANCELLED && target == VisitStatus.COMPLETED) {
            throw new RuntimeException("Cannot complete a cancelled visit");
        }
    }

    private LeadVisitResponse toResponse(LeadVisit v) {
        return LeadVisitResponse.builder()
                .id(v.getId())
                .uuid(v.getUuid())
                .orgId(v.getOrganization() != null ? v.getOrganization().getId() : null)
                .leadId(v.getLead() != null ? v.getLead().getId() : null)
                .leadCustomerName(v.getLead() != null ? v.getLead().getCustomerName() : null)
                .propertyId(v.getProperty() != null ? v.getProperty().getId() : null)
                .propertyName(v.getProperty() != null ? v.getProperty().getName() : null)
                .unitId(v.getUnit() != null ? v.getUnit().getId() : null)
                .unitNumber(v.getUnit() != null ? v.getUnit().getUnitNumber() : null)
                .scheduledAt(v.getScheduledAt())
                .visitedAt(v.getVisitedAt())
                .status(v.getStatus())
                .notes(v.getNotes())
                .feedback(v.getFeedback())
                .staffId(v.getStaff() != null ? v.getStaff().getId() : null)
                .staffName(v.getStaff() != null ? v.getStaff().getFullName() : null)
                .createdAt(v.getCreatedAt())
                .build();
    }
}
