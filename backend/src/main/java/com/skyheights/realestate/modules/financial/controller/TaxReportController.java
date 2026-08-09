package com.skyheights.realestate.modules.financial.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.financial.dto.TaxReportCreateRequest;
import com.skyheights.realestate.modules.financial.dto.TaxReportResponse;
import com.skyheights.realestate.modules.financial.service.TaxReportService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tax-reports")
@RequiredArgsConstructor
public class TaxReportController {

    private final TaxReportService reportService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('REPORT_VIEW') or @permEval.hasHierarchy(60)")
    public ResponseEntity<ApiResponse<TaxReportResponse>> generateReport(@CurrentUser UserPrincipal currentUser,
                                                                        @Valid @RequestBody TaxReportCreateRequest request) {
        TaxReportResponse resp = reportService.generateReport(currentUser.getOrgId(), currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Tax report generated - 1-click"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('REPORT_VIEW')")
    public ResponseEntity<ApiResponse<List<TaxReportResponse>>> getReports(@CurrentUser UserPrincipal currentUser) {
        List<TaxReportResponse> list = reportService.getReports(currentUser.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(list, "Tax reports fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('REPORT_VIEW')")
    public ResponseEntity<ApiResponse<TaxReportResponse>> getReport(@CurrentUser UserPrincipal currentUser,
                                                                    @PathVariable Long id) {
        TaxReportResponse resp = reportService.getReport(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Tax report fetched"));
    }

    @GetMapping("/fy/{financialYear}")
    @PreAuthorize("@permEval.hasPermission('REPORT_VIEW')")
    public ResponseEntity<ApiResponse<TaxReportResponse>> getByFy(@CurrentUser UserPrincipal currentUser,
                                                                 @PathVariable String financialYear) {
        TaxReportResponse resp = reportService.getReportByFinancialYear(currentUser.getOrgId(), financialYear);
        return ResponseEntity.ok(ApiResponse.success(resp, "Tax report fetched"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('REPORT_VIEW') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@CurrentUser UserPrincipal currentUser,
                                                         @PathVariable Long id) {
        reportService.deleteReport(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tax report deleted"));
    }
}
