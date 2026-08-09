package com.skyheights.realestate.modules.tenant.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.tenant.dto.ConditionReportCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.ConditionReportResponse;
import com.skyheights.realestate.modules.tenant.enums.ReportType;
import com.skyheights.realestate.modules.tenant.service.ConditionReportService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/condition-reports")
@RequiredArgsConstructor
public class ConditionReportController {

    private final ConditionReportService reportService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<ConditionReportResponse>> createReport(@CurrentUser UserPrincipal currentUser,
                                                                            @Valid @RequestBody ConditionReportCreateRequest request) {
        ConditionReportResponse resp = reportService.createReport(currentUser.getOrgId(), currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Condition report created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<Page<ConditionReportResponse>>> getReports(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long leaseId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) ReportType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ConditionReportResponse> page = reportService.searchReports(currentUser.getOrgId(), leaseId, unitId, tenantId, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Condition reports fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE') or @permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<ConditionReportResponse>> getReport(@CurrentUser UserPrincipal currentUser,
                                                                         @PathVariable Long id) {
        ConditionReportResponse resp = reportService.getReport(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Condition report fetched"));
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE')")
    public ResponseEntity<ApiResponse<ConditionReportResponse>> uploadPhotos(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam(required = false) Long itemId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) List<String> captions) {
        ConditionReportResponse resp = reportService.uploadPhotos(currentUser.getOrgId(), id, itemId, files, captions);
        return ResponseEntity.ok(ApiResponse.success(resp, "Photos uploaded to S3"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@permEval.hasPermission('LEASE_MANAGE')")
    public ResponseEntity<ApiResponse<ConditionReportResponse>> updateStatus(@CurrentUser UserPrincipal currentUser,
                                                                            @PathVariable Long id,
                                                                            @RequestParam String status) {
        ConditionReportResponse resp = reportService.updateReportStatus(currentUser.getOrgId(), id, status);
        return ResponseEntity.ok(ApiResponse.success(resp, "Report status updated"));
    }
}
