package com.skyheights.realestate.modules.tenant.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.tenant.dto.EsignCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.EsignResponse;
import com.skyheights.realestate.modules.tenant.service.EsignService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/esign")
@RequiredArgsConstructor
public class EsignController {

    private final EsignService esignService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('LEASE_ESIGN') or @permEval.hasPermission('LEASE_MANAGE')")
    public ResponseEntity<ApiResponse<EsignResponse>> createEsign(@CurrentUser UserPrincipal currentUser,
                                                                 @Valid @RequestBody EsignCreateRequest request) {
        EsignResponse resp = esignService.createEsignTracking(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Esign tracking created"));
    }

    @GetMapping("/lease/{leaseId}")
    @PreAuthorize("@permEval.hasPermission('LEASE_ESIGN') or @permEval.hasPermission('LEASE_MANAGE')")
    public ResponseEntity<ApiResponse<List<EsignResponse>>> getEsignsByLease(@CurrentUser UserPrincipal currentUser,
                                                                            @PathVariable Long leaseId) {
        List<EsignResponse> list = esignService.getEsignTrackings(currentUser.getOrgId(), leaseId);
        return ResponseEntity.ok(ApiResponse.success(list, "Esign trackings fetched"));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("@permEval.hasPermission('LEASE_ESIGN')")
    public ResponseEntity<ApiResponse<EsignResponse>> sendEsign(@CurrentUser UserPrincipal currentUser,
                                                               @PathVariable Long id) {
        EsignResponse resp = esignService.sendEsign(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Esign sent (mock OTP)"));
    }

    @PostMapping("/{id}/viewed")
    @PreAuthorize("@permEval.hasPermission('LEASE_ESIGN')")
    public ResponseEntity<ApiResponse<EsignResponse>> markViewed(@CurrentUser UserPrincipal currentUser,
                                                                @PathVariable Long id) {
        EsignResponse resp = esignService.markViewed(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Esign marked viewed"));
    }

    @PostMapping(value = "/{id}/sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permEval.hasPermission('LEASE_ESIGN')")
    public ResponseEntity<ApiResponse<EsignResponse>> signLease(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam(required = false) MultipartFile signatureFile,
            @RequestParam(required = false) String otp,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        EsignResponse resp = esignService.signLease(currentUser.getOrgId(), id, signatureFile, ip, userAgent, otp);
        return ResponseEntity.ok(ApiResponse.success(resp, "Lease signed"));
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("@permEval.hasPermission('LEASE_ESIGN')")
    public ResponseEntity<ApiResponse<EsignResponse>> declineEsign(@CurrentUser UserPrincipal currentUser,
                                                                   @PathVariable Long id,
                                                                   @RequestParam(required = false) String reason) {
        EsignResponse resp = esignService.declineEsign(currentUser.getOrgId(), id, reason);
        return ResponseEntity.ok(ApiResponse.success(resp, "Esign declined"));
    }
}
