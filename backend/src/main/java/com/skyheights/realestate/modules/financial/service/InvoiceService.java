package com.skyheights.realestate.modules.financial.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.financial.dto.InvoiceCreateRequest;
import com.skyheights.realestate.modules.financial.dto.InvoiceResponse;
import com.skyheights.realestate.modules.financial.dto.InvoiceUpdateRequest;
import com.skyheights.realestate.modules.financial.entity.Invoice;
import com.skyheights.realestate.modules.financial.entity.InvoiceLineItem;
import com.skyheights.realestate.modules.financial.entity.LateFeeRule;
import com.skyheights.realestate.modules.financial.enums.InvoiceStatus;
import com.skyheights.realestate.modules.financial.enums.InvoiceType;
import com.skyheights.realestate.modules.financial.enums.LateFeeType;
import com.skyheights.realestate.modules.financial.repository.InvoiceLineItemRepository;
import com.skyheights.realestate.modules.financial.repository.InvoiceRepository;
import com.skyheights.realestate.modules.financial.repository.LateFeeRuleRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.portfolio.repository.PropertyRepository;
import com.skyheights.realestate.modules.portfolio.repository.UnitRepository;
import com.skyheights.realestate.modules.tenant.entity.LeaseAgreement;
import com.skyheights.realestate.modules.tenant.entity.TenantProfile;
import com.skyheights.realestate.modules.tenant.repository.LeaseAgreementRepository;
import com.skyheights.realestate.modules.tenant.repository.TenantProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final TenantProfileRepository tenantRepository;
    private final LeaseAgreementRepository leaseRepository;
    private final OrganizationRepository organizationRepository;
    private final LateFeeRuleRepository lateFeeRuleRepository;
    private final S3Service s3Service;

    private static final AtomicLong invoiceCounter = new AtomicLong(System.currentTimeMillis() % 100000);

    @Transactional
    public InvoiceResponse createInvoice(Long orgId, InvoiceCreateRequest request) {
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

        LeaseAgreement lease = null;
        if (request.getLeaseId() != null) {
            lease = leaseRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getLeaseId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        }

        // Validate billing period
        if (request.getBillingPeriodStart() != null && request.getBillingPeriodEnd() != null &&
                request.getBillingPeriodStart().isAfter(request.getBillingPeriodEnd())) {
            throw new RuntimeException("Billing period start cannot be after end");
        }

        // Validate due date not before issue date
        if (request.getDueDate().isBefore(request.getIssueDate())) {
            throw new RuntimeException("Due date cannot be before issue date");
        }

        // Generate invoice number unique
        String invoiceNumber = generateInvoiceNumber();

        // Calculate subtotal from line items
        BigDecimal subtotal = BigDecimal.ZERO;
        List<InvoiceLineItem> lineItems = new ArrayList<>();
        if (request.getLineItems() != null && !request.getLineItems().isEmpty()) {
            for (var itemReq : request.getLineItems()) {
                BigDecimal qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;
                BigDecimal amount = itemReq.getUnitPrice().multiply(qty).setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(amount);
            }
        } else {
            // If no line items, use tenant's lease rent as base? For generic invoice, require at least one line
            throw new RuntimeException("At least one invoice line item required");
        }

        BigDecimal tax = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(tax).subtract(discount).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        Invoice invoice = Invoice.builder()
                .organization(org)
                .property(property)
                .unit(unit)
                .tenantId(tenant.getId())
                .leaseId(lease != null ? lease.getId() : null)
                .invoiceNumber(invoiceNumber)
                .type(request.getType())
                .billingPeriodStart(request.getBillingPeriodStart())
                .billingPeriodEnd(request.getBillingPeriodEnd())
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .subtotal(subtotal)
                .taxAmount(tax)
                .discountAmount(discount)
                .totalAmount(total)
                .amountPaid(BigDecimal.ZERO)
                .status(InvoiceStatus.DRAFT)
                .notes(request.getNotes())
                .autoGenerated(false)
                .build();

        invoice = invoiceRepository.save(invoice);

        // Save line items
        for (var itemReq : request.getLineItems()) {
            BigDecimal qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;
            BigDecimal amount = itemReq.getUnitPrice().multiply(qty).setScale(2, RoundingMode.HALF_UP);
            InvoiceLineItem lineItem = InvoiceLineItem.builder()
                    .invoice(invoice)
                    .description(itemReq.getDescription())
                    .quantity(qty)
                    .unitPrice(itemReq.getUnitPrice())
                    .amount(amount)
                    .type(itemReq.getType())
                    .build();
            lineItemRepository.save(lineItem);
        }

        // Refresh with line items
        invoice = invoiceRepository.findById(invoice.getId()).orElse(invoice);
        log.info("Created invoice {} type {} for tenant {} org {}", invoiceNumber, request.getType(), tenant.getId(), orgId);
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> searchInvoices(Long orgId, Long propertyId, Long unitId, Long tenantId, Long leaseId,
                                                InvoiceStatus status, InvoiceType type, String search, Pageable pageable) {
        Page<Invoice> page = invoiceRepository.search(orgId, propertyId, unitId, tenantId, leaseId, status, type, search, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long orgId, Long id) {
        Invoice invoice = invoiceRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse updateInvoice(Long orgId, Long id, InvoiceUpdateRequest request) {
        Invoice invoice = invoiceRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new RuntimeException("Cannot update invoice in status " + invoice.getStatus());
        }

        if (request.getBillingPeriodStart() != null) invoice.setBillingPeriodStart(request.getBillingPeriodStart());
        if (request.getBillingPeriodEnd() != null) invoice.setBillingPeriodEnd(request.getBillingPeriodEnd());
        if (request.getIssueDate() != null) invoice.setIssueDate(request.getIssueDate());
        if (request.getDueDate() != null) invoice.setDueDate(request.getDueDate());
        if (request.getTaxAmount() != null) invoice.setTaxAmount(request.getTaxAmount());
        if (request.getDiscountAmount() != null) invoice.setDiscountAmount(request.getDiscountAmount());
        if (request.getAmountPaid() != null) {
            if (request.getAmountPaid().compareTo(invoice.getTotalAmount()) > 0) {
                // Overpayment -> allow but log, will become credit
                log.warn("Overpayment for invoice {}: paid {} > total {}", invoice.getInvoiceNumber(), request.getAmountPaid(), invoice.getTotalAmount());
            }
            invoice.setAmountPaid(request.getAmountPaid());
            // Auto-update status
            if (request.getAmountPaid().compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus(InvoiceStatus.PAID);
            } else if (request.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
            }
        }
        if (request.getStatus() != null) {
            // Validate status transition
            validateStatusTransition(invoice.getStatus(), request.getStatus());
            invoice.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) invoice.setNotes(request.getNotes());

        // Recalculate total if line items provided
        if (request.getLineItems() != null) {
            lineItemRepository.deleteByInvoiceId(invoice.getId());
            BigDecimal subtotal = BigDecimal.ZERO;
            for (var itemReq : request.getLineItems()) {
                BigDecimal qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : BigDecimal.ONE;
                BigDecimal amount = itemReq.getUnitPrice().multiply(qty).setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(amount);
                InvoiceLineItem lineItem = InvoiceLineItem.builder()
                        .invoice(invoice)
                        .description(itemReq.getDescription())
                        .quantity(qty)
                        .unitPrice(itemReq.getUnitPrice())
                        .amount(amount)
                        .type(itemReq.getType())
                        .build();
                lineItemRepository.save(lineItem);
            }
            invoice.setSubtotal(subtotal);
            BigDecimal tax = invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO;
            BigDecimal discount = invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal late = invoice.getLateFeeAmount() != null ? invoice.getLateFeeAmount() : BigDecimal.ZERO;
            BigDecimal total = subtotal.add(tax).add(late).subtract(discount).setScale(2, RoundingMode.HALF_UP);
            invoice.setTotalAmount(total);
        } else {
            // Recalc total with existing subtotal + tax - discount + late
            BigDecimal subtotal = invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO;
            BigDecimal tax = invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO;
            BigDecimal discount = invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal late = invoice.getLateFeeAmount() != null ? invoice.getLateFeeAmount() : BigDecimal.ZERO;
            invoice.setTotalAmount(subtotal.add(tax).add(late).subtract(discount).setScale(2, RoundingMode.HALF_UP));
        }

        invoice = invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    @Transactional
    public void deleteInvoice(Long orgId, Long id) {
        Invoice invoice = invoiceRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new RuntimeException("Cannot delete PAID invoice, void it instead");
        }
        invoice.setIsDeleted(true);
        invoiceRepository.save(invoice);
        log.info("Soft deleted invoice {} org {}", id, orgId);
    }

    @Transactional
    public InvoiceResponse applyLateFee(Long orgId, Long id) {
        Invoice invoice = invoiceRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new RuntimeException("Cannot apply late fee to invoice in status " + invoice.getStatus());
        }

        BigDecimal lateFee = calculateLateFee(invoice);
        if (lateFee.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("No late fee applicable for invoice {} (due {}, today {})", invoice.getInvoiceNumber(), invoice.getDueDate(), LocalDate.now());
            return toResponse(invoice);
        }

        // Check if late fee line item already exists? For simplicity, update lateFeeAmount and total
        BigDecimal currentLate = invoice.getLateFeeAmount() != null ? invoice.getLateFeeAmount() : BigDecimal.ZERO;
        // Only keep max late fee, not cumulative per day? For PERCENTAGE_PER_DAY compounding, we calculate total from due date to today
        invoice.setLateFeeAmount(lateFee);

        BigDecimal newTotal = invoice.getSubtotal().add(invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO)
                .add(lateFee)
                .subtract(invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : BigDecimal.ZERO);
        invoice.setTotalAmount(newTotal.setScale(2, RoundingMode.HALF_UP));

        // Add line item for late fee if not exists
        boolean hasLateFeeItem = lineItemRepository.findByInvoiceId(invoice.getId()).stream()
                .anyMatch(li -> "LATE_FEE".equalsIgnoreCase(li.getType()));
        if (!hasLateFeeItem) {
            InvoiceLineItem lateItem = InvoiceLineItem.builder()
                    .invoice(invoice)
                    .description("Late fee for overdue invoice")
                    .quantity(BigDecimal.ONE)
                    .unitPrice(lateFee)
                    .amount(lateFee)
                    .type("LATE_FEE")
                    .build();
            lineItemRepository.save(lateItem);
        } else {
            // Update existing late fee line item amount
            lineItemRepository.findByInvoiceId(invoice.getId()).stream()
                    .filter(li -> "LATE_FEE".equalsIgnoreCase(li.getType()))
                    .findFirst()
                    .ifPresent(li -> {
                        li.setUnitPrice(lateFee);
                        li.setAmount(lateFee);
                        lineItemRepository.save(li);
                    });
        }

        if (LocalDate.now().isAfter(invoice.getDueDate())) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
        }

        invoice = invoiceRepository.save(invoice);
        log.info("Applied late fee {} to invoice {} org {}", lateFee, invoice.getInvoiceNumber(), orgId);
        return toResponse(invoice);
    }

    /**
     * Auto-generate rent invoices for all ACTIVE leases on 1st of month
     * Handles proration for mid-month move-in
     */
    @Transactional
    public int autoGenerateRentInvoicesForMonth(LocalDate monthDate) {
        YearMonth yearMonth = YearMonth.from(monthDate);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();
        LocalDate issueDate = periodStart;
        LocalDate dueDate = periodStart.plusDays(4); // due 5th

        List<LeaseAgreement> activeLeases = leaseRepository.findAll().stream()
                .filter(l -> l.getStatus() == com.skyheights.realestate.modules.tenant.enums.LeaseStatus.ACTIVE
                        && !Boolean.TRUE.equals(l.getIsDeleted())
                        && !l.getStartDate().isAfter(periodEnd)
                        && !l.getEndDate().isBefore(periodStart))
                .toList();

        int generated = 0;
        for (LeaseAgreement lease : activeLeases) {
            // Skip if invoice already exists for this lease+period+RENT
            if (invoiceRepository.existsByLeaseIdAndBillingPeriodStartAndBillingPeriodEndAndTypeAndIsDeletedFalse(
                    lease.getId(), periodStart, periodEnd, InvoiceType.RENT)) {
                continue;
            }

            // Calculate proration if lease starts mid-month
            BigDecimal rentAmount = lease.getRentAmount();
            String description = "Monthly rent for " + yearMonth.getMonth() + " " + yearMonth.getYear();
            BigDecimal quantity = BigDecimal.ONE;

            if (lease.getStartDate().isAfter(periodStart) && lease.getStartDate().getMonth() == periodStart.getMonth()) {
                // Prorate: remaining days / total days in month
                int daysInMonth = periodEnd.lengthOfMonth();
                int remainingDays = (int) ChronoUnit.DAYS.between(lease.getStartDate(), periodEnd) + 1;
                BigDecimal ratio = new BigDecimal(remainingDays).divide(new BigDecimal(daysInMonth), 4, RoundingMode.HALF_UP);
                rentAmount = lease.getRentAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                description = String.format("Prorated rent for %s %d (%d/%d days, move-in %s)",
                        yearMonth.getMonth(), yearMonth.getYear(), remainingDays, daysInMonth, lease.getStartDate());
                log.info("Proration for lease {}: {}/{} days, rent {} -> {}", lease.getLeaseNumber(), remainingDays, daysInMonth, lease.getRentAmount(), rentAmount);
            }

            // Check if lease ends mid-month and tenant leaves mid-month (notice)
            if (lease.getEndDate().isBefore(periodEnd) && lease.getEndDate().getMonth() == periodStart.getMonth()) {
                int daysInMonth = periodEnd.lengthOfMonth();
                int occupiedDays = (int) ChronoUnit.DAYS.between(periodStart, lease.getEndDate()) + 1;
                BigDecimal ratio = new BigDecimal(occupiedDays).divide(new BigDecimal(daysInMonth), 4, RoundingMode.HALF_UP);
                rentAmount = lease.getRentAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                description = String.format("Prorated rent for %s %d (%d/%d days, move-out %s)",
                        yearMonth.getMonth(), yearMonth.getYear(), occupiedDays, daysInMonth, lease.getEndDate());
            }

            try {
                InvoiceCreateRequest req = InvoiceCreateRequest.builder()
                        .propertyId(lease.getProperty().getId())
                        .unitId(lease.getUnit().getId())
                        .tenantId(lease.getTenant().getId())
                        .leaseId(lease.getId())
                        .type(InvoiceType.RENT)
                        .billingPeriodStart(periodStart)
                        .billingPeriodEnd(periodEnd)
                        .issueDate(issueDate)
                        .dueDate(dueDate)
                        .lineItems(List.of(
                                InvoiceCreateRequest.LineItemRequest.builder()
                                        .description(description)
                                        .quantity(quantity)
                                        .unitPrice(rentAmount)
                                        .type("RENT")
                                        .build()
                        ))
                        .build();

                // Direct save without orgId check? Use org from lease
                var orgId = lease.getOrganization().getId();
                // For auto-gen, we need to mimic createInvoice logic but we have orgId
                // Call internal method
                InvoiceResponse inv = createInvoice(orgId, req);
                // Mark autoGenerated true
                var invoiceEntity = invoiceRepository.findByInvoiceNumber(inv.getInvoiceNumber()).orElse(null);
                if (invoiceEntity != null) {
                    invoiceEntity.setAutoGenerated(true);
                    invoiceEntity.setStatus(InvoiceStatus.ISSUED);
                    invoiceRepository.save(invoiceEntity);
                }

                generated++;
            } catch (Exception e) {
                log.error("Failed to auto-generate rent invoice for lease {}: {}", lease.getLeaseNumber(), e.getMessage());
            }
        }

        log.info("Auto-generated {} rent invoices for {}", generated, yearMonth);
        return generated;
    }

    private BigDecimal calculateLateFee(Invoice invoice) {
        LocalDate today = LocalDate.now();
        if (!today.isAfter(invoice.getDueDate())) {
            return BigDecimal.ZERO;
        }

        long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
        // Find applicable late fee rule: property-specific first, then org-wide
        List<LateFeeRule> rules = new ArrayList<>();
        if (invoice.getProperty() != null) {
            rules.addAll(lateFeeRuleRepository.findByPropertyIdAndIsActiveTrueAndIsDeletedFalse(invoice.getProperty().getId()));
        }
        if (rules.isEmpty()) {
            rules.addAll(lateFeeRuleRepository.findByOrganizationIdAndIsActiveTrueAndIsDeletedFalse(invoice.getOrganization().getId()));
        }

        if (rules.isEmpty()) {
            // Default: fixed 100 per day after grace 3 days? For fallback, 0
            return BigDecimal.ZERO;
        }

        LateFeeRule rule = rules.get(0); // Use first active rule
        long effectiveDays = daysOverdue - (rule.getGracePeriodDays() != null ? rule.getGracePeriodDays() : 0);
        if (effectiveDays <= 0) return BigDecimal.ZERO;

        BigDecimal lateFee;
        switch (rule.getFeeType()) {
            case FIXED:
                lateFee = rule.getAmountValue() != null ? rule.getAmountValue() : BigDecimal.ZERO;
                break;
            case PERCENTAGE_PER_DAY:
                BigDecimal rate = rule.getPercentageRate() != null ? rule.getPercentageRate() : BigDecimal.ZERO;
                // lateFee = totalAmount * rate% * days
                lateFee = invoice.getTotalAmount()
                        .multiply(rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP))
                        .multiply(new BigDecimal(effectiveDays));
                break;
            case SLAB:
                // Simplified slab: amountValue per slab? For now fixed
                lateFee = rule.getAmountValue() != null ? rule.getAmountValue().multiply(new BigDecimal(effectiveDays)) : BigDecimal.ZERO;
                break;
            default:
                lateFee = BigDecimal.ZERO;
        }

        // Cap
        if (rule.getMaxCapAmount() != null && lateFee.compareTo(rule.getMaxCapAmount()) > 0) {
            lateFee = rule.getMaxCapAmount();
        }

        return lateFee.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateStatusTransition(InvoiceStatus current, InvoiceStatus target) {
        if (current == target) return;
        switch (current) {
            case DRAFT:
                if (target != InvoiceStatus.ISSUED && target != InvoiceStatus.CANCELLED) {
                    throw new RuntimeException("DRAFT can only go to ISSUED or CANCELLED");
                }
                break;
            case ISSUED:
                if (target != InvoiceStatus.PAID && target != InvoiceStatus.PARTIALLY_PAID && target != InvoiceStatus.OVERDUE && target != InvoiceStatus.CANCELLED) {
                    throw new RuntimeException("ISSUED can only go to PAID, PARTIALLY_PAID, OVERDUE, CANCELLED");
                }
                break;
            case PARTIALLY_PAID:
                if (target != InvoiceStatus.PAID && target != InvoiceStatus.OVERDUE && target != InvoiceStatus.CANCELLED) {
                    throw new RuntimeException("PARTIALLY_PAID can only go to PAID, OVERDUE, CANCELLED");
                }
                break;
            case OVERDUE:
                if (target != InvoiceStatus.PAID && target != InvoiceStatus.PARTIALLY_PAID && target != InvoiceStatus.CANCELLED) {
                    throw new RuntimeException("OVERDUE can only go to PAID, PARTIALLY_PAID, CANCELLED");
                }
                break;
            case PAID:
            case CANCELLED:
            case VOID:
                throw new RuntimeException(current + " is terminal, cannot transition to " + target);
        }
    }

    private String generateInvoiceNumber() {
        long next = invoiceCounter.incrementAndGet();
        int year = LocalDate.now().getYear();
        String candidate;
        do {
            candidate = String.format("INV-%d-%05d", year, next % 100000);
            next = invoiceCounter.incrementAndGet();
        } while (invoiceRepository.findByInvoiceNumber(candidate).isPresent());
        invoiceCounter.set(next);
        return candidate;
    }

    private InvoiceResponse toResponse(Invoice i) {
        List<InvoiceResponse.LineItemResponse> lineItems = lineItemRepository.findByInvoiceId(i.getId()).stream()
                .map(li -> InvoiceResponse.LineItemResponse.builder()
                        .id(li.getId())
                        .description(li.getDescription())
                        .quantity(li.getQuantity())
                        .unitPrice(li.getUnitPrice())
                        .amount(li.getAmount())
                        .type(li.getType())
                        .build())
                .collect(Collectors.toList());

        long daysOverdue = 0;
        boolean overdue = false;
        if (i.getDueDate() != null && LocalDate.now().isAfter(i.getDueDate()) &&
                (i.getStatus() == InvoiceStatus.ISSUED || i.getStatus() == InvoiceStatus.PARTIALLY_PAID || i.getStatus() == InvoiceStatus.OVERDUE)) {
            daysOverdue = ChronoUnit.DAYS.between(i.getDueDate(), LocalDate.now());
            overdue = true;
        }

        String presigned = null;
        try {
            if (i.getPdfS3Key() != null) {
                presigned = s3Service.generatePresignedUrl(i.getPdfS3Key(), Duration.ofMinutes(30));
            }
        } catch (Exception e) {
            // ignore
        }

        // balanceDue is generated column, but calculate fallback
        BigDecimal balance = i.getTotalAmount() != null && i.getAmountPaid() != null ? i.getTotalAmount().subtract(i.getAmountPaid()) : i.getTotalAmount();

        return InvoiceResponse.builder()
                .id(i.getId())
                .uuid(i.getUuid())
                .orgId(i.getOrganization() != null ? i.getOrganization().getId() : null)
                .propertyId(i.getProperty() != null ? i.getProperty().getId() : null)
                .propertyName(i.getProperty() != null ? i.getProperty().getName() : null)
                .unitId(i.getUnit() != null ? i.getUnit().getId() : null)
                .unitNumber(i.getUnit() != null ? i.getUnit().getUnitNumber() : null)
                .tenantId(i.getTenantId())
                .tenantName(null) // would need tenant repo lookup, simplified
                .leaseId(i.getLeaseId())
                .invoiceNumber(i.getInvoiceNumber())
                .type(i.getType())
                .billingPeriodStart(i.getBillingPeriodStart())
                .billingPeriodEnd(i.getBillingPeriodEnd())
                .issueDate(i.getIssueDate())
                .dueDate(i.getDueDate())
                .subtotal(i.getSubtotal())
                .taxAmount(i.getTaxAmount())
                .lateFeeAmount(i.getLateFeeAmount())
                .discountAmount(i.getDiscountAmount())
                .totalAmount(i.getTotalAmount())
                .amountPaid(i.getAmountPaid())
                .balanceDue(balance)
                .status(i.getStatus())
                .notes(i.getNotes())
                .pdfS3Key(i.getPdfS3Key())
                .pdfPresignedUrl(presigned)
                .autoGenerated(i.getAutoGenerated())
                .lineItems(lineItems)
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .daysOverdue(daysOverdue)
                .overdue(overdue)
                .build();
    }
}
