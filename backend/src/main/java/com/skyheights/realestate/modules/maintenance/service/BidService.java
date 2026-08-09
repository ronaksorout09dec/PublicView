package com.skyheights.realestate.modules.maintenance.service;

import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.maintenance.dto.BidCreateRequest;
import com.skyheights.realestate.modules.maintenance.dto.BidResponse;
import com.skyheights.realestate.modules.maintenance.entity.MaintenanceTicket;
import com.skyheights.realestate.modules.maintenance.entity.VendorBid;
import com.skyheights.realestate.modules.maintenance.entity.VendorProfile;
import com.skyheights.realestate.modules.maintenance.enums.BidStatus;
import com.skyheights.realestate.modules.maintenance.enums.TicketStatus;
import com.skyheights.realestate.modules.maintenance.repository.MaintenanceTicketRepository;
import com.skyheights.realestate.modules.maintenance.repository.VendorBidRepository;
import com.skyheights.realestate.modules.maintenance.repository.VendorProfileRepository;
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
public class BidService {

    private final VendorBidRepository bidRepository;
    private final MaintenanceTicketRepository ticketRepository;
    private final VendorProfileRepository vendorRepository;

    @Transactional
    public BidResponse submitBid(Long orgId, Long vendorUserId, BidCreateRequest request) {
        MaintenanceTicket ticket = ticketRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getTicketId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        // Only BIDDING or BROADCASTED tickets can receive bids
        if (ticket.getStatus() != TicketStatus.BIDDING && ticket.getStatus() != TicketStatus.BROADCASTED) {
            throw new RuntimeException("Ticket not open for bidding, current status: " + ticket.getStatus());
        }

        VendorProfile vendor = null;
        if (vendorUserId != null) {
            // Find vendor profile by userId
            vendor = vendorRepository.findByUserIdAndIsDeletedFalse(vendorUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found for user"));
            if (!vendor.getOrganization().getId().equals(orgId)) {
                throw new RuntimeException("Vendor does not belong to org");
            }
        } else {
            // For manager creating bid on behalf? Not allowed, but we will require vendor user
            throw new RuntimeException("Vendor user ID required for bidding");
        }

        if (bidRepository.existsByTicketIdAndVendorIdAndIsDeletedFalse(ticket.getId(), vendor.getId())) {
            throw new RuntimeException("Vendor already submitted bid for this ticket");
        }

        // Check specialization matches? Warn but allow
        try {
            var spec = com.skyheights.realestate.modules.maintenance.enums.VendorSpecialization.valueOf(ticket.getCategory());
            if (vendor.getSpecialization() != spec) {
                log.warn("Vendor specialization {} does not match ticket category {} for ticket {}",
                        vendor.getSpecialization(), ticket.getCategory(), ticket.getId());
            }
        } catch (Exception e) {
            log.warn("Ticket category {} not valid specialization", ticket.getCategory());
        }

        VendorBid bid = VendorBid.builder()
                .organization(ticket.getOrganization())
                .ticket(ticket)
                .vendor(vendor)
                .bidAmount(request.getBidAmount())
                .estimatedDays(request.getEstimatedDays())
                .proposal(request.getProposal())
                .status(BidStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .includesMaterial(request.getIncludesMaterial() != null ? request.getIncludesMaterial() : false)
                .warrantyDays(request.getWarrantyDays() != null ? request.getWarrantyDays() : 0)
                .build();

        bid = bidRepository.save(bid);
        log.info("Vendor {} submitted bid {} amount {} for ticket {} org {}", vendor.getId(), bid.getId(), bid.getBidAmount(), ticket.getId(), orgId);
        return toResponse(bid);
    }

    @Transactional(readOnly = true)
    public Page<BidResponse> getBidsForTicket(Long orgId, Long ticketId, Pageable pageable) {
        ticketRepository.findByIdAndOrganizationIdAndIsDeletedFalse(ticketId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        Page<VendorBid> page = bidRepository.findByTicketIdAndIsDeletedFalse(ticketId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BidResponse getBid(Long orgId, Long bidId) {
        VendorBid bid = bidRepository.findByIdAndOrganizationIdAndIsDeletedFalse(bidId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));
        return toResponse(bid);
    }

    @Transactional
    public BidResponse approveBid(Long orgId, Long bidId, Long managerUserId) {
        VendorBid bid = bidRepository.findByIdAndOrganizationIdAndIsDeletedFalse(bidId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));

        MaintenanceTicket ticket = bid.getTicket();

        if (ticket.getStatus() != TicketStatus.BIDDING && ticket.getStatus() != TicketStatus.BROADCASTED) {
            throw new RuntimeException("Ticket not in BIDDING state, current: " + ticket.getStatus());
        }

        if (bid.getStatus() != BidStatus.SUBMITTED) {
            throw new RuntimeException("Only SUBMITTED bids can be approved");
        }

        // Approve this bid
        bid.setStatus(BidStatus.APPROVED);
        bid.setApprovedAt(Instant.now());
        bidRepository.save(bid);

        // Reject all other bids for same ticket
        var otherBids = bidRepository.findByTicketIdAndIsDeletedFalse(ticket.getId()).stream()
                .filter(b -> !b.getId().equals(bidId) && b.getStatus() == BidStatus.SUBMITTED)
                .toList();
        for (var other : otherBids) {
            other.setStatus(BidStatus.REJECTED);
            other.setRejectionReason("Another bid approved");
            bidRepository.save(other);
        }

        // Assign vendor to ticket
        ticket.setAssignedVendor(bid.getVendor());
        ticket.setAssignedBidId(bid.getId());
        ticket.setEstimatedCost(bid.getBidAmount());
        ticket.setStatus(TicketStatus.ASSIGNED);
        ticketRepository.save(ticket);

        log.info("Approved bid {} for ticket {} assigned vendor {} org {}", bidId, ticket.getId(), bid.getVendor().getId(), orgId);
        return toResponse(bid);
    }

    @Transactional
    public BidResponse rejectBid(Long orgId, Long bidId, String reason) {
        VendorBid bid = bidRepository.findByIdAndOrganizationIdAndIsDeletedFalse(bidId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));

        if (bid.getStatus() != BidStatus.SUBMITTED) {
            throw new RuntimeException("Only SUBMITTED bids can be rejected");
        }

        bid.setStatus(BidStatus.REJECTED);
        bid.setRejectionReason(reason != null ? reason : "Rejected by manager");
        bidRepository.save(bid);

        log.info("Rejected bid {} for ticket {} org {} reason {}", bidId, bid.getTicket().getId(), orgId, reason);
        return toResponse(bid);
    }

    @Transactional
    public BidResponse withdrawBid(Long orgId, Long bidId, Long vendorUserId) {
        VendorBid bid = bidRepository.findByIdAndOrganizationIdAndIsDeletedFalse(bidId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));

        // Verify vendor owns bid
        if (vendorUserId != null) {
            var vendor = vendorRepository.findByUserIdAndIsDeletedFalse(vendorUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
            if (!bid.getVendor().getId().equals(vendor.getId())) {
                throw new RuntimeException("Vendor does not own this bid");
            }
        }

        if (bid.getStatus() != BidStatus.SUBMITTED) {
            throw new RuntimeException("Only SUBMITTED bids can be withdrawn");
        }

        bid.setStatus(BidStatus.WITHDRAWN);
        bidRepository.save(bid);
        log.info("Vendor withdrew bid {} org {}", bidId, orgId);
        return toResponse(bid);
    }

    private BidResponse toResponse(VendorBid b) {
        return BidResponse.builder()
                .id(b.getId()).uuid(b.getUuid()).orgId(b.getOrganization() != null ? b.getOrganization().getId() : null)
                .ticketId(b.getTicket() != null ? b.getTicket().getId() : null)
                .ticketTitle(b.getTicket() != null ? b.getTicket().getTitle() : null)
                .vendorId(b.getVendor() != null ? b.getVendor().getId() : null)
                .vendorCompanyName(b.getVendor() != null ? b.getVendor().getCompanyName() : null)
                .vendorName(b.getVendor() != null && b.getVendor().getUser() != null ? b.getVendor().getUser().getFullName() : null)
                .bidAmount(b.getBidAmount()).estimatedDays(b.getEstimatedDays()).proposal(b.getProposal())
                .status(b.getStatus()).submittedAt(b.getSubmittedAt()).approvedAt(b.getApprovedAt())
                .rejectionReason(b.getRejectionReason()).includesMaterial(b.getIncludesMaterial())
                .warrantyDays(b.getWarrantyDays()).createdAt(b.getCreatedAt())
                .build();
    }
}
