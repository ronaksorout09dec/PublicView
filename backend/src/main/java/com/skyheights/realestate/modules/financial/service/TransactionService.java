package com.skyheights.realestate.modules.financial.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.financial.dto.TransactionCreateRequest;
import com.skyheights.realestate.modules.financial.dto.TransactionResponse;
import com.skyheights.realestate.modules.financial.entity.Invoice;
import com.skyheights.realestate.modules.financial.entity.Transaction;
import com.skyheights.realestate.modules.financial.enums.TransactionCategory;
import com.skyheights.realestate.modules.financial.enums.TransactionType;
import com.skyheights.realestate.modules.financial.repository.InvoiceRepository;
import com.skyheights.realestate.modules.financial.repository.TransactionRepository;
import com.skyheights.realestate.modules.organization.entity.AppUser;
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

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final InvoiceRepository invoiceRepository;
    private final AppUserRepository appUserRepository;
    private final S3Service s3Service;

    @Transactional
    public TransactionResponse createTransaction(Long orgId, Long actorUserId, TransactionCreateRequest request) {
        var org = organizationRepository.findById(orgId)
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
        }

        Invoice invoice = null;
        if (request.getInvoiceId() != null) {
            invoice = invoiceRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getInvoiceId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        }

        AppUser actor = appUserRepository.findByIdAndIsDeletedFalse(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Actor user not found"));

        Transaction transaction = Transaction.builder()
                .organization(org)
                .property(property)
                .unit(unit)
                .type(request.getType())
                .category(request.getCategory())
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .paymentMethod(request.getPaymentMethod())
                .invoice(invoice)
                .vendorPayoutId(request.getVendorPayoutId())
                .ledgerReferenceType(request.getLedgerReferenceType())
                .ledgerReferenceId(request.getLedgerReferenceId())
                .receiptS3Key(request.getReceiptS3Key())
                .createdByUser(actor)
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Created transaction {} type {} category {} amount {} org {}", transaction.getId(), request.getType(), request.getCategory(), request.getAmount(), orgId);
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> searchTransactions(Long orgId, Long propertyId, TransactionType type, TransactionCategory category,
                                                        java.time.LocalDate start, java.time.LocalDate end, Pageable pageable) {
        Page<Transaction> page = transactionRepository.search(orgId, propertyId, type, category, start, end, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long orgId, Long id) {
        Transaction tx = transactionRepository.findById(id)
                .filter(t -> t.getOrganization().getId().equals(orgId) && !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return toResponse(tx);
    }

    @Transactional
    public void deleteTransaction(Long orgId, Long id) {
        Transaction tx = transactionRepository.findById(id)
                .filter(t -> t.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        tx.setIsDeleted(true);
        transactionRepository.save(tx);
        log.info("Soft deleted transaction {} org {}", id, orgId);
    }

    private TransactionResponse toResponse(Transaction t) {
        String presigned = null;
        try {
            if (t.getReceiptS3Key() != null) presigned = s3Service.generatePresignedUrl(t.getReceiptS3Key(), Duration.ofMinutes(15));
        } catch (Exception ignored) {}

        return TransactionResponse.builder()
                .id(t.getId()).uuid(t.getUuid())
                .orgId(t.getOrganization() != null ? t.getOrganization().getId() : null)
                .propertyId(t.getProperty() != null ? t.getProperty().getId() : null)
                .propertyName(t.getProperty() != null ? t.getProperty().getName() : null)
                .unitId(t.getUnit() != null ? t.getUnit().getId() : null)
                .unitNumber(t.getUnit() != null ? t.getUnit().getUnitNumber() : null)
                .type(t.getType()).category(t.getCategory())
                .amount(t.getAmount()).date(t.getDate())
                .description(t.getDescription()).paymentMethod(t.getPaymentMethod())
                .invoiceId(t.getInvoice() != null ? t.getInvoice().getId() : null)
                .invoiceNumber(t.getInvoice() != null ? t.getInvoice().getInvoiceNumber() : null)
                .vendorPayoutId(t.getVendorPayoutId())
                .ledgerReferenceType(t.getLedgerReferenceType())
                .ledgerReferenceId(t.getLedgerReferenceId())
                .receiptS3Key(t.getReceiptS3Key()).receiptPresignedUrl(presigned)
                .createdByUserId(t.getCreatedByUser() != null ? t.getCreatedByUser().getId() : null)
                .createdByName(t.getCreatedByUser() != null ? t.getCreatedByUser().getFullName() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
