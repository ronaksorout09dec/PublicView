package com.skyheights.realestate.modules.financial.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.financial.dto.DepositLedgerCreateRequest;
import com.skyheights.realestate.modules.financial.dto.SecurityDepositResponse;
import com.skyheights.realestate.modules.financial.entity.SecurityDeposit;
import com.skyheights.realestate.modules.financial.entity.SecurityDepositLedger;
import com.skyheights.realestate.modules.financial.enums.DepositLedgerType;
import com.skyheights.realestate.modules.financial.enums.DepositStatus;
import com.skyheights.realestate.modules.financial.repository.SecurityDepositLedgerRepository;
import com.skyheights.realestate.modules.financial.repository.SecurityDepositRepository;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.tenant.entity.LeaseAgreement;
import com.skyheights.realestate.modules.tenant.repository.LeaseAgreementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityDepositService {

    private final SecurityDepositRepository depositRepository;
    private final SecurityDepositLedgerRepository ledgerRepository;
    private final LeaseAgreementRepository leaseRepository;
    private final AppUserRepository appUserRepository;
    private final S3Service s3Service;

    @Transactional
    public SecurityDepositResponse createDepositForLease(Long orgId, Long leaseId) {
        LeaseAgreement lease = leaseRepository.findByIdAndOrganizationIdAndIsDeletedFalse(leaseId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        if (depositRepository.findByLeaseIdAndIsDeletedFalse(leaseId).isPresent()) {
            throw new RuntimeException("Deposit already exists for lease " + lease.getLeaseNumber());
        }

        SecurityDeposit deposit = SecurityDeposit.builder()
                .organization(lease.getOrganization())
                .leaseId(lease.getId())
                .tenantId(lease.getTenant().getId())
                .unit(lease.getUnit())
                .totalDeposited(lease.getDepositAmount())
                .currency("INR")
                .status(DepositStatus.HELD)
                .build();

        deposit = depositRepository.save(deposit);

        // Initial ledger entry DEPOSIT
        SecurityDepositLedger ledger = SecurityDepositLedger.builder()
                .deposit(deposit)
                .transactionType(DepositLedgerType.DEPOSIT)
                .description("Initial deposit for lease " + lease.getLeaseNumber())
                .amount(lease.getDepositAmount())
                .balanceAfter(lease.getDepositAmount())
                .build();

        ledgerRepository.save(ledger);

        log.info("Created security deposit {} for lease {} org {}", deposit.getId(), lease.getLeaseNumber(), orgId);
        return toResponse(deposit);
    }

    @Transactional(readOnly = true)
    public SecurityDepositResponse getDepositByLease(Long orgId, Long leaseId) {
        SecurityDeposit deposit = depositRepository.findByLeaseIdAndIsDeletedFalse(leaseId)
                .filter(d -> d.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Deposit not found for lease"));
        return toResponse(deposit);
    }

    @Transactional(readOnly = true)
    public SecurityDepositResponse getDeposit(Long orgId, Long depositId) {
        SecurityDeposit deposit = depositRepository.findByIdAndOrganizationIdAndIsDeletedFalse(depositId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit not found"));
        return toResponse(deposit);
    }

    @Transactional
    public SecurityDepositResponse addLedgerEntry(Long orgId, Long actorUserId, DepositLedgerCreateRequest request) {
        SecurityDeposit deposit = depositRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getDepositId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit not found"));

        var actor = appUserRepository.findByIdAndIsDeletedFalse(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Actor user not found"));

        // Calculate new balance
        BigDecimal currentBalance = getCurrentBalance(deposit.getId());
        BigDecimal newBalance;

        switch (request.getTransactionType()) {
            case DEPOSIT:
                // Positive amount adds to balance
                if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Deposit amount must be positive");
                newBalance = currentBalance.add(request.getAmount());
                break;
            case DEDUCTION:
            case FORFEITURE:
                // Deduction reduces balance, amount should be positive but we subtract
                if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Deduction amount must be positive");
                if (currentBalance.compareTo(request.getAmount()) < 0) {
                    throw new RuntimeException("Insufficient deposit balance. Current: " + currentBalance + ", deduction: " + request.getAmount());
                }
                newBalance = currentBalance.subtract(request.getAmount());
                // For ledger, store negative amount for deduction
                request.setAmount(request.getAmount().negate());
                break;
            case REFUND:
                // Refund reduces balance as well (returning money), amount positive but subtract
                if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Refund amount must be positive");
                if (currentBalance.compareTo(request.getAmount()) < 0) {
                    throw new RuntimeException("Insufficient balance for refund. Current: " + currentBalance);
                }
                newBalance = currentBalance.subtract(request.getAmount());
                request.setAmount(request.getAmount().negate());
                break;
            case ADJUSTMENT:
                // Adjustment can be positive or negative
                newBalance = currentBalance.add(request.getAmount());
                break;
            default:
                throw new RuntimeException("Unsupported transaction type");
        }

        SecurityDepositLedger ledger = SecurityDepositLedger.builder()
                .deposit(deposit)
                .transactionType(request.getTransactionType())
                .description(request.getDescription())
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .createdBy(actor)
                .receiptS3Key(request.getReceiptS3Key())
                .build();

        ledgerRepository.save(ledger);

        // Update deposit status if balance zero → REFUNDED, if partial → PARTIALLY_REFUNDED
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            if (request.getTransactionType() == DepositLedgerType.FORFEITURE) {
                deposit.setStatus(DepositStatus.FORFEITED);
            } else {
                deposit.setStatus(DepositStatus.REFUNDED);
            }
        } else if (newBalance.compareTo(deposit.getTotalDeposited()) < 0) {
            deposit.setStatus(DepositStatus.PARTIALLY_REFUNDED);
        }

        depositRepository.save(deposit);

        log.info("Deposit ledger {} {} amount {} balance {} -> {} org {}", request.getTransactionType(), deposit.getId(), request.getAmount(), currentBalance, newBalance, orgId);
        return toResponse(deposit);
    }

    private BigDecimal getCurrentBalance(Long depositId) {
        var entries = ledgerRepository.findByDepositIdOrderByCreatedAtDesc(depositId);
        if (entries.isEmpty()) return BigDecimal.ZERO;
        return entries.get(0).getBalanceAfter();
    }

    private SecurityDepositResponse toResponse(SecurityDeposit deposit) {
        List<SecurityDepositLedger> ledgers = ledgerRepository.findByDepositIdOrderByCreatedAtDesc(deposit.getId());
        BigDecimal currentBalance = ledgers.isEmpty() ? deposit.getTotalDeposited() : ledgers.get(0).getBalanceAfter();

        List<SecurityDepositResponse.LedgerResponse> ledgerResponses = ledgers.stream().map(l -> {
            String presigned = null;
            try {
                if (l.getReceiptS3Key() != null) presigned = s3Service.generatePresignedUrl(l.getReceiptS3Key(), Duration.ofMinutes(15));
            } catch (Exception ignored) {}
            return SecurityDepositResponse.LedgerResponse.builder()
                    .id(l.getId()).uuid(l.getUuid())
                    .transactionType(l.getTransactionType().name())
                    .description(l.getDescription())
                    .amount(l.getAmount())
                    .balanceAfter(l.getBalanceAfter())
                    .referenceType(l.getReferenceType())
                    .referenceId(l.getReferenceId())
                    .createdByUserId(l.getCreatedBy() != null ? l.getCreatedBy().getId() : null)
                    .createdByName(l.getCreatedBy() != null ? l.getCreatedBy().getFullName() : null)
                    .receiptS3Key(l.getReceiptS3Key())
                    .receiptPresignedUrl(presigned)
                    .createdAt(l.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        return SecurityDepositResponse.builder()
                .id(deposit.getId()).uuid(deposit.getUuid())
                .orgId(deposit.getOrganization() != null ? deposit.getOrganization().getId() : null)
                .leaseId(deposit.getLeaseId())
                .leaseNumber(null) // could lookup
                .tenantId(deposit.getTenantId())
                .tenantName(null)
                .unitId(deposit.getUnit() != null ? deposit.getUnit().getId() : null)
                .unitNumber(deposit.getUnit() != null ? deposit.getUnit().getUnitNumber() : null)
                .totalDeposited(deposit.getTotalDeposited())
                .currency(deposit.getCurrency())
                .status(deposit.getStatus())
                .heldInAccount(deposit.getHeldInAccount())
                .createdAt(deposit.getCreatedAt())
                .currentBalance(currentBalance)
                .ledgerEntries(ledgerResponses)
                .build();
    }
}
