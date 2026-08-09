package com.skyheights.realestate.modules.maintenance.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.financial.entity.Transaction;
import com.skyheights.realestate.modules.financial.enums.TransactionCategory;
import com.skyheights.realestate.modules.financial.enums.TransactionType;
import com.skyheights.realestate.modules.financial.repository.TransactionRepository;
import com.skyheights.realestate.modules.maintenance.dto.PayoutCreateRequest;
import com.skyheights.realestate.modules.maintenance.dto.PayoutResponse;
import com.skyheights.realestate.modules.maintenance.entity.VendorPayout;
import com.skyheights.realestate.modules.maintenance.entity.WorkOrder;
import com.skyheights.realestate.modules.maintenance.enums.PayoutStatus;
import com.skyheights.realestate.modules.maintenance.enums.WorkOrderStatus;
import com.skyheights.realestate.modules.maintenance.repository.VendorPayoutRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutService {

    private final VendorPayoutRepository payoutRepository;
    private final WorkOrderRepository workOrderRepository;
    private final TransactionRepository transactionRepository;
    private final AppUserRepository appUserRepository;
    private final S3Service s3Service;

    @Transactional
    public PayoutResponse createPayout(Long orgId, PayoutCreateRequest request) {
        WorkOrder workOrder = workOrderRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getWorkOrderId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found"));

        if (workOrder.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new RuntimeException("Payout can only be created for COMPLETED work orders");
        }

        // Check if payout already exists for work order
        boolean exists = payoutRepository.findByVendorIdAndIsDeletedFalse(workOrder.getVendor().getId()).stream()
                .anyMatch(p -> p.getWorkOrder().getId().equals(workOrder.getId()) && p.getStatus() != PayoutStatus.FAILED);
        if (exists) {
            throw new RuntimeException("Payout already exists for work order " + request.getWorkOrderId());
        }

        BigDecimal amount = workOrder.getBid() != null ? workOrder.getBid().getBidAmount() : workOrder.getTicket().getEstimatedCost();
        if (amount == null) throw new RuntimeException("Cannot determine payout amount from work order");

        BigDecimal tds = request.getTdsDeducted() != null ? request.getTdsDeducted() : BigDecimal.ZERO;
        BigDecimal net = amount.subtract(tds).setScale(2, RoundingMode.HALF_UP);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;

        VendorPayout payout = VendorPayout.builder()
                .organization(workOrder.getOrganization())
                .workOrder(workOrder)
                .ticket(workOrder.getTicket())
                .vendor(workOrder.getVendor())
                .amount(amount)
                .tdsDeducted(tds)
                .netPayable(net)
                .status(PayoutStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();

        payout = payoutRepository.save(payout);
        log.info("Created payout {} amount {} net {} for work order {} org {}", payout.getId(), amount, net, workOrder.getId(), orgId);
        return toResponse(payout);
    }

    @Transactional(readOnly = true)
    public Page<PayoutResponse> searchPayouts(Long orgId, Long vendorId, PayoutStatus status, Pageable pageable) {
        Page<VendorPayout> page = payoutRepository.search(orgId, vendorId, status, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PayoutResponse getPayout(Long orgId, Long id) {
        VendorPayout payout = payoutRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));
        return toResponse(payout);
    }

    @Transactional
    public PayoutResponse approvePayout(Long orgId, Long id) {
        VendorPayout payout = payoutRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new RuntimeException("Only PENDING payouts can be approved");
        }

        payout.setStatus(PayoutStatus.APPROVED);
        payoutRepository.save(payout);
        log.info("Approved payout {} org {}", id, orgId);
        return toResponse(payout);
    }

    @Transactional
    public PayoutResponse markPaid(Long orgId, Long id, Long paidByUserId, String utr, String paymentMethod, MultipartFile invoiceFile) {
        VendorPayout payout = payoutRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));

        if (payout.getStatus() != PayoutStatus.APPROVED && payout.getStatus() != PayoutStatus.PENDING) {
            throw new RuntimeException("Only APPROVED/PENDING payouts can be marked PAID");
        }

        AppUser paidBy = appUserRepository.findByIdAndIsDeletedFalse(paidByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Paid by user not found"));

        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidBy(paidBy);
        payout.setPaidAt(Instant.now());
        payout.setUtrNumber(utr);
        if (paymentMethod != null) payout.setPaymentMethod(paymentMethod);

        if (invoiceFile != null && !invoiceFile.isEmpty()) {
            try {
                String key = s3Service.generateKey(orgId, "payouts/" + id, invoiceFile.getOriginalFilename());
                String s3Key = s3Service.uploadFile(key, invoiceFile.getInputStream(), invoiceFile.getSize(), invoiceFile.getContentType());
                payout.setInvoiceS3Key(s3Key);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload payout invoice: " + e.getMessage());
            }
        }

        // Create transaction for accounting
        Transaction transaction = Transaction.builder()
                .organization(payout.getOrganization())
                .property(payout.getTicket() != null ? payout.getTicket().getProperty() : null)
                .unit(payout.getTicket() != null ? payout.getTicket().getUnit() : null)
                .type(TransactionType.EXPENSE)
                .category(TransactionCategory.VENDOR_PAYOUT)
                .amount(payout.getNetPayable())
                .date(java.time.LocalDate.now())
                .description("Vendor payout for ticket " + payout.getTicket().getId() + " work order " + payout.getWorkOrder().getId())
                .paymentMethod(payout.getPaymentMethod())
                .createdByUser(paidBy)
                .build();
        transaction = transactionRepository.save(transaction);
        payout.setTransaction(transaction);

        payout = payoutRepository.save(payout);
        log.info("Marked payout {} PAID UTR {} org {} transaction {}", id, utr, orgId, transaction.getId());
        return toResponse(payout);
    }

    @Transactional
    public PayoutResponse failPayout(Long orgId, Long id, String reason) {
        VendorPayout payout = payoutRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found"));

        payout.setStatus(PayoutStatus.FAILED);
        payout.setNotes((payout.getNotes() != null ? payout.getNotes() + " | " : "") + "FAILED: " + reason);
        payoutRepository.save(payout);
        log.warn("Payout {} FAILED org {} reason {}", id, orgId, reason);
        return toResponse(payout);
    }

    private PayoutResponse toResponse(VendorPayout p) {
        String presigned = null;
        try {
            if (p.getInvoiceS3Key() != null) presigned = s3Service.generatePresignedUrl(p.getInvoiceS3Key(), Duration.ofMinutes(30));
        } catch (Exception ignored) {}

        return PayoutResponse.builder()
                .id(p.getId()).uuid(p.getUuid())
                .orgId(p.getOrganization() != null ? p.getOrganization().getId() : null)
                .workOrderId(p.getWorkOrder() != null ? p.getWorkOrder().getId() : null)
                .ticketId(p.getTicket() != null ? p.getTicket().getId() : null)
                .ticketTitle(p.getTicket() != null ? p.getTicket().getTitle() : null)
                .vendorId(p.getVendor() != null ? p.getVendor().getId() : null)
                .vendorCompanyName(p.getVendor() != null ? p.getVendor().getCompanyName() : null)
                .amount(p.getAmount()).tdsDeducted(p.getTdsDeducted()).netPayable(p.getNetPayable())
                .status(p.getStatus()).paymentMethod(p.getPaymentMethod()).utrNumber(p.getUtrNumber())
                .transactionId(p.getTransaction() != null ? p.getTransaction().getId() : null)
                .paidAt(p.getPaidAt())
                .paidByUserId(p.getPaidBy() != null ? p.getPaidBy().getId() : null)
                .paidByName(p.getPaidBy() != null ? p.getPaidBy().getFullName() : null)
                .notes(p.getNotes()).invoiceS3Key(p.getInvoiceS3Key()).invoicePresignedUrl(presigned)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
