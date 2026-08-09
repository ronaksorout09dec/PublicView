package com.skyheights.realestate.modules.financial.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.financial.dto.TaxReportCreateRequest;
import com.skyheights.realestate.modules.financial.dto.TaxReportResponse;
import com.skyheights.realestate.modules.financial.entity.TaxReportSnapshot;
import com.skyheights.realestate.modules.financial.enums.TransactionType;
import com.skyheights.realestate.modules.financial.repository.TaxReportSnapshotRepository;
import com.skyheights.realestate.modules.financial.repository.TransactionRepository;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxReportService {

    private final TaxReportSnapshotRepository reportRepository;
    private final TransactionRepository transactionRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @Transactional
    public TaxReportResponse generateReport(Long orgId, Long actorUserId, TaxReportCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Start date cannot be after end date");
        }

        if (reportRepository.existsByOrganizationIdAndFinancialYearAndIsDeletedFalse(orgId, request.getFinancialYear())) {
            throw new RuntimeException("Tax report already exists for financial year " + request.getFinancialYear());
        }

        AppUser actor = appUserRepository.findByIdAndIsDeletedFalse(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Actor user not found"));

        // Calculate totals from transactions
        BigDecimal totalIncome = transactionRepository.sumByOrgAndTypeAndDateBetween(orgId, TransactionType.INCOME, request.getStartDate(), request.getEndDate());
        BigDecimal totalExpense = transactionRepository.sumByOrgAndTypeAndDateBetween(orgId, TransactionType.EXPENSE, request.getStartDate(), request.getEndDate());

        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;

        // Build detailed JSON summary (simplified)
        Map<String, Object> summary = new HashMap<>();
        summary.put("financialYear", request.getFinancialYear());
        summary.put("startDate", request.getStartDate().toString());
        summary.put("endDate", request.getEndDate().toString());
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("netProfit", totalIncome.subtract(totalExpense));

        // For demo, category breakdown empty - in prod would query group by category/property/month
        summary.put("incomeByCategory", Map.of());
        summary.put("expenseByCategory", Map.of());
        summary.put("propertyBreakdown", Map.of());

        String reportJson;
        try {
            reportJson = objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize report JSON");
        }

        TaxReportSnapshot report = TaxReportSnapshot.builder()
                .organization(org)
                .financialYear(request.getFinancialYear())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .totalTds(BigDecimal.ZERO) // TODO calculate from transactions if TDS category exists
                .totalGst(BigDecimal.ZERO)
                .reportJson(reportJson)
                .generatedBy(actor)
                .build();

        report = reportRepository.save(report);

        // TODO: Generate PDF and upload to S3, set reportPdfS3Key
        // For Phase 3, mock PDF generation - in prod would use iText or similar to generate PDF from reportJson
        log.info("Generated tax report {} FY {} org {} income {} expense {} profit {}",
                report.getId(), request.getFinancialYear(), orgId, totalIncome, totalExpense, totalIncome.subtract(totalExpense));

        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public List<TaxReportResponse> getReports(Long orgId) {
        return reportRepository.findByOrganizationIdAndIsDeletedFalse(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaxReportResponse getReport(Long orgId, Long id) {
        TaxReportSnapshot report = reportRepository.findById(id)
                .filter(r -> r.getOrganization().getId().equals(orgId) && !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Tax report not found"));
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public TaxReportResponse getReportByFinancialYear(Long orgId, String financialYear) {
        TaxReportSnapshot report = reportRepository.findByOrganizationIdAndFinancialYearAndIsDeletedFalse(orgId, financialYear)
                .orElseThrow(() -> new ResourceNotFoundException("Tax report not found for FY " + financialYear));
        return toResponse(report);
    }

    @Transactional
    public void deleteReport(Long orgId, Long id) {
        TaxReportSnapshot report = reportRepository.findById(id)
                .filter(r -> r.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Tax report not found"));
        report.setIsDeleted(true);
        reportRepository.save(report);
        log.info("Soft deleted tax report {} org {}", id, orgId);
    }

    private TaxReportResponse toResponse(TaxReportSnapshot r) {
        String presigned = null;
        try {
            if (r.getReportPdfS3Key() != null) presigned = s3Service.generatePresignedUrl(r.getReportPdfS3Key(), Duration.ofMinutes(30));
        } catch (Exception ignored) {}

        return TaxReportResponse.builder()
                .id(r.getId()).uuid(r.getUuid())
                .orgId(r.getOrganization() != null ? r.getOrganization().getId() : null)
                .financialYear(r.getFinancialYear())
                .startDate(r.getStartDate()).endDate(r.getEndDate())
                .totalIncome(r.getTotalIncome()).totalExpense(r.getTotalExpense())
                .netProfit(r.getTotalIncome() != null && r.getTotalExpense() != null ? r.getTotalIncome().subtract(r.getTotalExpense()) : null)
                .totalTds(r.getTotalTds()).totalGst(r.getTotalGst())
                .reportJson(r.getReportJson())
                .reportPdfS3Key(r.getReportPdfS3Key())
                .reportPdfPresignedUrl(presigned)
                .generatedAt(r.getGeneratedAt())
                .generatedByUserId(r.getGeneratedBy() != null ? r.getGeneratedBy().getId() : null)
                .generatedByName(r.getGeneratedBy() != null ? r.getGeneratedBy().getFullName() : null)
                .createdAt(r.getCreatedAt())
                .build();
    }
}
