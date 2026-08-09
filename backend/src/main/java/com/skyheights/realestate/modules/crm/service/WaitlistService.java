package com.skyheights.realestate.modules.crm.service;

import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.crm.dto.WaitlistCreateRequest;
import com.skyheights.realestate.modules.crm.dto.WaitlistResponse;
import com.skyheights.realestate.modules.crm.entity.CrmLead;
import com.skyheights.realestate.modules.crm.entity.WaitlistEntry;
import com.skyheights.realestate.modules.crm.enums.WaitlistStatus;
import com.skyheights.realestate.modules.crm.repository.CrmLeadRepository;
import com.skyheights.realestate.modules.crm.repository.WaitlistEntryRepository;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    private final WaitlistEntryRepository waitlistRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final CrmLeadRepository leadRepository;

    @Transactional
    public WaitlistResponse addToWaitlist(Long orgId, WaitlistCreateRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        CrmLead lead = leadRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getLeadId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        // Edge: prevent duplicate active waitlist for same lead+property+unitType
        if (waitlistRepository.existsByLeadIdAndPropertyIdAndStatusAndIsDeletedFalse(lead.getId(), property.getId(), WaitlistStatus.WAITING)) {
            throw new RuntimeException("Lead already in waiting list for this property");
        }
        if (waitlistRepository.existsByLeadIdAndPropertyIdAndStatusAndIsDeletedFalse(lead.getId(), property.getId(), WaitlistStatus.OFFERED)) {
            throw new RuntimeException("Lead already offered a unit for this property");
        }

        int maxPos = waitlistRepository.findMaxPositionByPropertyId(property.getId());
        int newPos = maxPos + 1;

        // Priority score default: if not provided, use 0 + maybe budget? For now 0
        // Future: priorityScore = budgetMax/10000 + timeline urgency etc
        int priorityScore = request.getPriorityScore() != null ? request.getPriorityScore() : 0;

        WaitlistEntry entry = WaitlistEntry.builder()
                .organization(org)
                .property(property)
                .unitType(request.getUnitType())
                .lead(lead)
                .position(newPos)
                .status(WaitlistStatus.WAITING)
                .priorityScore(priorityScore)
                .desiredMoveIn(request.getDesiredMoveIn())
                .build();

        entry = waitlistRepository.save(entry);
        log.info("Added lead {} to waitlist property {} position {} org {}", lead.getId(), property.getId(), newPos, orgId);
        return toResponse(entry);
    }

    @Transactional(readOnly = true)
    public Page<WaitlistResponse> searchWaitlist(Long orgId, Long propertyId, String unitType, WaitlistStatus status, Pageable pageable) {
        Page<WaitlistEntry> page = waitlistRepository.search(orgId, propertyId, unitType, status, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WaitlistResponse getEntry(Long orgId, Long id) {
        WaitlistEntry entry = waitlistRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found"));
        return toResponse(entry);
    }

    @Transactional
    public WaitlistResponse updateStatus(Long orgId, Long id, WaitlistStatus newStatus) {
        WaitlistEntry entry = waitlistRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found"));

        // State machine
        WaitlistStatus current = entry.getStatus();
        if (current == newStatus) return toResponse(entry);

        switch (current) {
            case WAITING:
                if (newStatus != WaitlistStatus.OFFERED && newStatus != WaitlistStatus.CANCELLED && newStatus != WaitlistStatus.EXPIRED) {
                    throw new RuntimeException("From WAITING only OFFERED, CANCELLED, EXPIRED allowed");
                }
                break;
            case OFFERED:
                if (newStatus != WaitlistStatus.ACCEPTED && newStatus != WaitlistStatus.EXPIRED && newStatus != WaitlistStatus.CANCELLED) {
                    throw new RuntimeException("From OFFERED only ACCEPTED, EXPIRED, CANCELLED allowed");
                }
                break;
            case ACCEPTED:
                throw new RuntimeException("ACCEPTED is terminal, cannot change");
            case EXPIRED:
            case CANCELLED:
                if (newStatus != WaitlistStatus.WAITING) {
                    throw new RuntimeException("From " + current + " only WAITING (re-queue) allowed");
                }
                // Re-queue: assign new position at end
                int maxPos = waitlistRepository.findMaxPositionByPropertyId(entry.getProperty().getId());
                entry.setPosition(maxPos + 1);
                break;
        }

        entry.setStatus(newStatus);
        entry = waitlistRepository.save(entry);
        log.info("Waitlist {} status {} -> {} org {}", id, current, newStatus, orgId);
        return toResponse(entry);
    }

    @Transactional
    public void removeFromWaitlist(Long orgId, Long id) {
        WaitlistEntry entry = waitlistRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found"));
        entry.setIsDeleted(true);
        waitlistRepository.save(entry);
        log.info("Soft deleted waitlist {} org {}", id, orgId);
    }

    /**
     * When a unit becomes VACANT, this method should be called to offer to top waitlist entry
     * For Phase 3.1, expose as service method for future automation
     */
    @Transactional(readOnly = true)
    public WaitlistResponse getNextInLineForProperty(Long propertyId, String unitType) {
        return waitlistRepository.findByPropertyIdAndUnitTypeAndStatusAndIsDeletedFalseOrderByPriorityScoreDescPositionAsc(propertyId, unitType, WaitlistStatus.WAITING)
                .stream()
                .findFirst()
                .map(this::toResponse)
                .orElse(null);
    }

    private WaitlistResponse toResponse(WaitlistEntry e) {
        return WaitlistResponse.builder()
                .id(e.getId())
                .uuid(e.getUuid())
                .orgId(e.getOrganization() != null ? e.getOrganization().getId() : null)
                .propertyId(e.getProperty() != null ? e.getProperty().getId() : null)
                .propertyName(e.getProperty() != null ? e.getProperty().getName() : null)
                .unitType(e.getUnitType())
                .leadId(e.getLead() != null ? e.getLead().getId() : null)
                .leadCustomerName(e.getLead() != null ? e.getLead().getCustomerName() : null)
                .leadPhone(e.getLead() != null ? e.getLead().getPhone() : null)
                .position(e.getPosition())
                .status(e.getStatus())
                .priorityScore(e.getPriorityScore())
                .desiredMoveIn(e.getDesiredMoveIn())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
