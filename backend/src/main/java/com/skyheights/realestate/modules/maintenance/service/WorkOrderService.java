package com.skyheights.realestate.modules.maintenance.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.maintenance.dto.WorkOrderResponse;
import com.skyheights.realestate.modules.maintenance.entity.MaintenanceTicket;
import com.skyheights.realestate.modules.maintenance.entity.VendorBid;
import com.skyheights.realestate.modules.maintenance.entity.WorkOrder;
import com.skyheights.realestate.modules.maintenance.enums.WorkOrderStatus;
import com.skyheights.realestate.modules.maintenance.enums.TicketStatus;
import com.skyheights.realestate.modules.maintenance.repository.MaintenanceTicketRepository;
import com.skyheights.realestate.modules.maintenance.repository.VendorBidRepository;
import com.skyheights.realestate.modules.maintenance.repository.WorkOrderRepository;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final MaintenanceTicketRepository ticketRepository;
    private final VendorBidRepository bidRepository;
    private final AppUserRepository appUserRepository;
    private final S3Service s3Service;

    @Transactional
    public WorkOrderResponse createWorkOrder(Long orgId, Long ticketId, Long bidId, Long assignedByUserId, LocalDate scheduledDate) {
        MaintenanceTicket ticket = ticketRepository.findByIdAndOrganizationIdAndIsDeletedFalse(ticketId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new RuntimeException("Work order can only be created for ASSIGNED tickets, current: " + ticket.getStatus());
        }

        if (workOrderRepository.findByTicketIdAndIsDeletedFalse(ticketId).isPresent()) {
            throw new RuntimeException("Work order already exists for ticket " + ticketId);
        }

        VendorBid bid = bidRepository.findByIdAndOrganizationIdAndIsDeletedFalse(bidId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));

        if (!bid.getTicket().getId().equals(ticketId)) {
            throw new RuntimeException("Bid does not belong to ticket");
        }

        AppUser assignedBy = appUserRepository.findByIdAndIsDeletedFalse(assignedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigner user not found"));

        WorkOrder workOrder = WorkOrder.builder()
                .organization(ticket.getOrganization())
                .ticket(ticket)
                .vendor(bid.getVendor())
                .bid(bid)
                .assignedBy(assignedBy)
                .status(WorkOrderStatus.CREATED)
                .scheduledDate(scheduledDate)
                .checklistCompleted(false)
                .otpVerifiedForCompletion(false)
                .build();

        workOrder = workOrderRepository.save(workOrder);

        // Update ticket to IN_PROGRESS
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);

        log.info("Created work order {} for ticket {} org {}", workOrder.getId(), ticketId, orgId);
        return toResponse(workOrder);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> searchWorkOrders(Long orgId, Long vendorId, WorkOrderStatus status, Pageable pageable) {
        Page<WorkOrder> page = workOrderRepository.search(orgId, vendorId, status, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse getWorkOrder(Long orgId, Long id) {
        WorkOrder wo = workOrderRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found"));
        return toResponse(wo);
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse getByTicket(Long orgId, Long ticketId) {
        WorkOrder wo = workOrderRepository.findByTicketIdAndIsDeletedFalse(ticketId)
                .filter(w -> w.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found for ticket"));
        return toResponse(wo);
    }

    @Transactional
    public WorkOrderResponse updateStatus(Long orgId, Long id, WorkOrderStatus newStatus, String completionNotes) {
        WorkOrder wo = workOrderRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found"));

        validateTransition(wo.getStatus(), newStatus);

        WorkOrderStatus old = wo.getStatus();
        wo.setStatus(newStatus);

        if (newStatus == WorkOrderStatus.IN_PROGRESS && wo.getStartDate() == null) {
            wo.setStartDate(Instant.now());
        }
        if (newStatus == WorkOrderStatus.COMPLETED) {
            wo.setCompletedDate(Instant.now());
            wo.setCompletionNotes(completionNotes);
            // Update ticket to COMPLETED
            MaintenanceTicket ticket = wo.getTicket();
            ticket.setStatus(TicketStatus.COMPLETED);
            ticket.setCompletedAt(Instant.now());
            ticket.setActualCost(wo.getBid() != null ? wo.getBid().getBidAmount() : ticket.getEstimatedCost());
            ticket.setCompletionNotes(completionNotes);
            ticketRepository.save(ticket);
            log.info("Work order {} COMPLETED -> ticket {} COMPLETED org {}", id, ticket.getId(), orgId);
        }

        wo = workOrderRepository.save(wo);
        log.info("Work order {} status {} -> {} org {}", id, old, newStatus, orgId);
        return toResponse(wo);
    }

    @Transactional
    public WorkOrderResponse uploadInvoice(Long orgId, Long id, MultipartFile invoiceFile) {
        WorkOrder wo = workOrderRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found"));

        if (invoiceFile == null || invoiceFile.isEmpty()) {
            throw new RuntimeException("Invoice file required");
        }

        try {
            String key = s3Service.generateKey(orgId, "maintenance/workorders/" + id, invoiceFile.getOriginalFilename());
            String s3Key = s3Service.uploadFile(key, invoiceFile.getInputStream(), invoiceFile.getSize(), invoiceFile.getContentType());
            wo.setInvoiceS3Key(s3Key);
            wo = workOrderRepository.save(wo);
            log.info("Uploaded invoice {} for work order {} org {}", s3Key, id, orgId);
            return toResponse(wo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload invoice: " + e.getMessage());
        }
    }

    private void validateTransition(WorkOrderStatus current, WorkOrderStatus target) {
        if (current == target) return;
        switch (current) {
            case CREATED:
                if (target != WorkOrderStatus.ACCEPTED && target != WorkOrderStatus.CANCELLED) {
                    throw new RuntimeException("CREATED can only go to ACCEPTED/CANCELLED");
                }
                break;
            case ACCEPTED:
                if (target != WorkOrderStatus.IN_PROGRESS && target != WorkOrderStatus.CANCELLED) {
                    throw new RuntimeException("ACCEPTED can only go to IN_PROGRESS/CANCELLED");
                }
                break;
            case IN_PROGRESS:
                if (target != WorkOrderStatus.PENDING_APPROVAL && target != WorkOrderStatus.COMPLETED && target != WorkOrderStatus.CANCELLED) {
                    throw new RuntimeException("IN_PROGRESS can only go to PENDING_APPROVAL/COMPLETED/CANCELLED");
                }
                break;
            case PENDING_APPROVAL:
                if (target != WorkOrderStatus.COMPLETED && target != WorkOrderStatus.IN_PROGRESS) {
                    throw new RuntimeException("PENDING_APPROVAL can only go to COMPLETED/IN_PROGRESS");
                }
                break;
            case COMPLETED:
            case CANCELLED:
                throw new RuntimeException(current + " is terminal");
        }
    }

    private WorkOrderResponse toResponse(WorkOrder w) {
        String presigned = null;
        try {
            if (w.getInvoiceS3Key() != null) presigned = s3Service.generatePresignedUrl(w.getInvoiceS3Key(), Duration.ofMinutes(30));
        } catch (Exception ignored) {}

        return WorkOrderResponse.builder()
                .id(w.getId()).uuid(w.getUuid())
                .orgId(w.getOrganization() != null ? w.getOrganization().getId() : null)
                .ticketId(w.getTicket() != null ? w.getTicket().getId() : null)
                .ticketTitle(w.getTicket() != null ? w.getTicket().getTitle() : null)
                .vendorId(w.getVendor() != null ? w.getVendor().getId() : null)
                .vendorCompanyName(w.getVendor() != null ? w.getVendor().getCompanyName() : null)
                .bidId(w.getBid() != null ? w.getBid().getId() : null)
                .assignedByUserId(w.getAssignedBy() != null ? w.getAssignedBy().getId() : null)
                .assignedByName(w.getAssignedBy() != null ? w.getAssignedBy().getFullName() : null)
                .status(w.getStatus())
                .scheduledDate(w.getScheduledDate()).startDate(w.getStartDate()).completedDate(w.getCompletedDate())
                .completionNotes(w.getCompletionNotes()).checklistCompleted(w.getChecklistCompleted())
                .otpVerifiedForCompletion(w.getOtpVerifiedForCompletion())
                .invoiceS3Key(w.getInvoiceS3Key()).invoicePresignedUrl(presigned)
                .createdAt(w.getCreatedAt()).updatedAt(w.getUpdatedAt())
                .build();
    }
}
