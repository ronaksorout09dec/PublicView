package com.skyheights.realestate.modules.crm.service;

import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.crm.dto.CrmLeadCreateRequest;
import com.skyheights.realestate.modules.crm.dto.CrmLeadResponse;
import com.skyheights.realestate.modules.crm.dto.CrmLeadUpdateRequest;
import com.skyheights.realestate.modules.crm.entity.CrmLead;
import com.skyheights.realestate.modules.crm.enums.LeadSource;
import com.skyheights.realestate.modules.crm.enums.LeadStatus;
import com.skyheights.realestate.modules.crm.repository.CrmLeadRepository;
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

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrmLeadService {

    private final CrmLeadRepository leadRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public CrmLeadResponse createLead(Long orgId, CrmLeadCreateRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Property property = null;
        if (request.getPropertyId() != null) {
            property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found in org"));
        }

        Unit unit = null;
        if (request.getUnitId() != null) {
            unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUnitId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found in org"));
            if (property != null && !unit.getProperty().getId().equals(property.getId())) {
                throw new RuntimeException("Unit does not belong to given property");
            }
        }

        AppUser assignedTo = null;
        if (request.getAssignedToStaffId() != null) {
            assignedTo = appUserRepository.findByIdAndIsDeletedFalse(request.getAssignedToStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff user not found"));
            Long staffOrgId = assignedTo.getOrganization() != null ? assignedTo.getOrganization().getId() : assignedTo.getOrgId();
            if (!staffOrgId.equals(orgId)) {
                throw new RuntimeException("Assigned staff must belong to same org");
            }
        }

        // Edge: budget min <= max
        if (request.getBudgetMin() != null && request.getBudgetMax() != null &&
                request.getBudgetMin().compareTo(request.getBudgetMax()) > 0) {
            throw new RuntimeException("Budget min cannot be greater than max");
        }

        // Edge: duplicate phone per property? Check
        if (request.getPropertyId() != null &&
                leadRepository.existsByOrganizationIdAndPhoneAndPropertyIdAndIsDeletedFalse(orgId, request.getPhone(), request.getPropertyId())) {
            log.warn("Duplicate lead phone {} for property {}", request.getPhone(), request.getPropertyId());
            // Allow duplicate but log - could be same person interested again
        }

        // AI score validation
        if (request.getAiScore() != null && (request.getAiScore().compareTo(BigDecimal.ZERO) < 0 || request.getAiScore().compareTo(new BigDecimal("10")) > 0)) {
            throw new RuntimeException("AI score must be between 0 and 10");
        }

        CrmLead lead = CrmLead.builder()
                .organization(org)
                .property(property)
                .unit(unit)
                .interestedUnitType(request.getInterestedUnitType())
                .customerName(request.getCustomerName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .source(request.getSource() != null ? request.getSource() : LeadSource.WEBSITE)
                .status(LeadStatus.NEW)
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .configuration(request.getConfiguration())
                .timeline(request.getTimeline())
                .purpose(request.getPurpose())
                .assignedTo(assignedTo)
                .notes(request.getNotes())
                .conversationSummary(request.getConversationSummary())
                .nextFollowupAt(request.getNextFollowupAt())
                .aiScore(request.getAiScore())
                .build();

        lead = leadRepository.save(lead);
        log.info("Created CRM lead {} for org {}", lead.getId(), orgId);
        return toResponse(lead);
    }

    @Transactional(readOnly = true)
    public Page<CrmLeadResponse> searchLeads(Long orgId, LeadStatus status, LeadSource source, Long propertyId, Long assignedTo, String search, String priority, Pageable pageable) {
        Page<CrmLead> page = leadRepository.search(orgId, status, source, propertyId, assignedTo, search, priority, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CrmLeadResponse getLead(Long orgId, Long id) {
        CrmLead lead = leadRepository.findByIdWithVisits(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        return toResponse(lead);
    }

    @Transactional
    public CrmLeadResponse updateLead(Long orgId, Long id, CrmLeadUpdateRequest request) {
        CrmLead lead = leadRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        if (request.getPropertyId() != null) {
            Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
            lead.setProperty(property);
        }
        if (request.getUnitId() != null) {
            Unit unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUnitId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            lead.setUnit(unit);
        }
        if (request.getInterestedUnitType() != null) lead.setInterestedUnitType(request.getInterestedUnitType());
        if (request.getCustomerName() != null) lead.setCustomerName(request.getCustomerName());
        if (request.getPhone() != null) lead.setPhone(request.getPhone());
        if (request.getEmail() != null) lead.setEmail(request.getEmail());
        if (request.getSource() != null) lead.setSource(request.getSource());
        if (request.getStatus() != null) {
            validateStatusTransition(lead.getStatus(), request.getStatus());
            lead.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) lead.setPriority(request.getPriority());
        if (request.getBudgetMin() != null) lead.setBudgetMin(request.getBudgetMin());
        if (request.getBudgetMax() != null) lead.setBudgetMax(request.getBudgetMax());
        if (request.getConfiguration() != null) lead.setConfiguration(request.getConfiguration());
        if (request.getTimeline() != null) lead.setTimeline(request.getTimeline());
        if (request.getPurpose() != null) lead.setPurpose(request.getPurpose());
        if (request.getNotes() != null) lead.setNotes(request.getNotes());
        if (request.getConversationSummary() != null) lead.setConversationSummary(request.getConversationSummary());
        if (request.getLostReason() != null) lead.setLostReason(request.getLostReason());
        if (request.getNextFollowupAt() != null) lead.setNextFollowupAt(request.getNextFollowupAt());
        if (request.getAiScore() != null) lead.setAiScore(request.getAiScore());

        if (request.getAssignedToStaffId() != null) {
            AppUser staff = appUserRepository.findByIdAndIsDeletedFalse(request.getAssignedToStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
            lead.setAssignedTo(staff);
        }

        // Budget edge re-validate
        if (lead.getBudgetMin() != null && lead.getBudgetMax() != null && lead.getBudgetMin().compareTo(lead.getBudgetMax()) > 0) {
            throw new RuntimeException("Budget min cannot be greater than max");
        }

        lead = leadRepository.save(lead);
        log.info("Updated lead {} org {}", id, orgId);
        return toResponse(lead);
    }

    @Transactional
    public void deleteLead(Long orgId, Long id) {
        CrmLead lead = leadRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        lead.setIsDeleted(true);
        leadRepository.save(lead);
        log.info("Soft deleted lead {} org {}", id, orgId);
    }

    private void validateStatusTransition(LeadStatus current, LeadStatus target) {
        if (current == target) return;
        // Allow all transitions except: CONVERTED, LOST, JUNK are terminal? But allow reopen
        if (current == LeadStatus.CONVERTED && target != LeadStatus.CONVERTED) {
            // Allow to move from CONVERTED to others? Better prevent accidental
            log.warn("Lead status transition from CONVERTED to {} - allowed but unusual", target);
        }
        if (current == LeadStatus.LOST && target == LeadStatus.CONVERTED) {
            throw new RuntimeException("Cannot convert a LOST lead. Create new lead or move to NEW first.");
        }
        // No other strict blocking for now
    }

    private CrmLeadResponse toResponse(CrmLead l) {
        return CrmLeadResponse.builder()
                .id(l.getId())
                .uuid(l.getUuid())
                .orgId(l.getOrganization() != null ? l.getOrganization().getId() : null)
                .propertyId(l.getProperty() != null ? l.getProperty().getId() : null)
                .propertyName(l.getProperty() != null ? l.getProperty().getName() : null)
                .unitId(l.getUnit() != null ? l.getUnit().getId() : null)
                .unitNumber(l.getUnit() != null ? l.getUnit().getUnitNumber() : null)
                .interestedUnitType(l.getInterestedUnitType())
                .customerName(l.getCustomerName())
                .phone(l.getPhone())
                .email(l.getEmail())
                .source(l.getSource())
                .status(l.getStatus())
                .priority(l.getPriority())
                .budgetMin(l.getBudgetMin())
                .budgetMax(l.getBudgetMax())
                .configuration(l.getConfiguration())
                .timeline(l.getTimeline())
                .purpose(l.getPurpose())
                .assignedToStaffId(l.getAssignedTo() != null ? l.getAssignedTo().getId() : null)
                .assignedToStaffName(l.getAssignedTo() != null ? l.getAssignedTo().getFullName() : null)
                .notes(l.getNotes())
                .conversationSummary(l.getConversationSummary())
                .lostReason(l.getLostReason())
                .nextFollowupAt(l.getNextFollowupAt())
                .aiScore(l.getAiScore())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .visitsCount(l.getVisits() != null ? l.getVisits().size() : 0)
                .build();
    }
}
