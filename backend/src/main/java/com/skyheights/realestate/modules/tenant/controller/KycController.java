package com.skyheights.realestate.modules.tenant.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.tenant.dto.KycCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.KycResponse;
import com.skyheights.realestate.modules.tenant.enums.KycDocumentType;
import com.skyheights.realestate.modules.tenant.service.KycService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('TENANT_WRITE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<KycResponse>> createKyc(@CurrentUser UserPrincipal currentUser,
                                                             @Valid @RequestBody KycCreateRequest request) {
        KycResponse resp = kycService.createKycDocument(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "KYC document created"));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permEval.hasPermission('TENANT_WRITE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<KycResponse>> uploadKyc(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam Long tenantId,
            @RequestParam KycDocumentType documentType,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(required = false) MultipartFile frontFile,
            @RequestParam(required = false) MultipartFile backFile) {
        KycResponse resp = kycService.uploadAndCreate(currentUser.getOrgId(), tenantId, documentType, documentNumber, frontFile, backFile, expiryDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "KYC uploaded to S3"));
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("@permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<List<KycResponse>>> getKycByTenant(@CurrentUser UserPrincipal currentUser,
                                                                        @PathVariable Long tenantId) {
        List<KycResponse> list = kycService.getKycByTenant(currentUser.getOrgId(), tenantId);
        return ResponseEntity.ok(ApiResponse.success(list, "KYC documents fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<KycResponse>> getKyc(@CurrentUser UserPrincipal currentUser,
                                                          @PathVariable Long id) {
        KycResponse resp = kycService.getKycDocument(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "KYC fetched"));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("@permEval.hasPermission('KYC_VERIFY') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<KycResponse>> verifyKyc(@CurrentUser UserPrincipal currentUser,
                                                              @PathVariable Long id,
                                                              @RequestParam boolean approved,
                                                              @RequestParam(required = false) String rejectionReason) {
        KycResponse resp = kycService.verifyKyc(currentUser.getOrgId(), id, currentUser.getId(), approved, rejectionReason);
        return ResponseEntity.ok(ApiResponse.success(resp, approved ? "KYC verified" : "KYC rejected"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('KYC_VERIFY') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<Void>> deleteKyc(@CurrentUser UserPrincipal currentUser,
                                                      @PathVariable Long id) {
        kycService.deleteKyc(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "KYC deleted"));
    }
}
