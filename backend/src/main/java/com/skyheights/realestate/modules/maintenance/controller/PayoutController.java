package com.skyheights.realestate.modules.maintenance.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.maintenance.dto.PayoutCreateRequest;
import com.skyheights.realestate.modules.maintenance.dto.PayoutResponse;
import com.skyheights.realestate.modules.maintenance.enums.PayoutStatus;
import com.skyheights.realestate.modules.maintenance.service.PayoutService;
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

@RestController
@RequestMapping("/api/vendor/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('VENDOR_PAYOUT_MANAGE') or @permEval.hasHierarchy(60)")
    public ResponseEntity<ApiResponse<PayoutResponse>> createPayout(@CurrentUser UserPrincipal currentUser,
                                                                   @Valid @RequestBody PayoutCreateRequest request) {
        PayoutResponse resp = payoutService.createPayout(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Payout created for COMPLETED work order"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('VENDOR_PAYOUT_MANAGE') or @permEval.hasPermission('VENDOR_BID')")
    public ResponseEntity<ApiResponse<Page<PayoutResponse>>> getPayouts(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) PayoutStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PayoutResponse> page = payoutService.searchPayouts(currentUser.getOrgId(), vendorId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Payouts fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('VENDOR_PAYOUT_MANAGE') or @permEval.hasPermission('VENDOR_BID')")
    public ResponseEntity<ApiResponse<PayoutResponse>> getPayout(@CurrentUser UserPrincipal currentUser,
                                                                @PathVariable Long id) {
        PayoutResponse resp = payoutService.getPayout(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Payout fetched"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@permEval.hasPermission('VENDOR_PAYOUT_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<PayoutResponse>> approvePayout(@CurrentUser UserPrincipal currentUser,
                                                                    @PathVariable Long id) {
        PayoutResponse resp = payoutService.approvePayout(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Payout approved"));
    }

    @PostMapping(value = "/{id}/pay", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permEval.hasPermission('VENDOR_PAYOUT_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<PayoutResponse>> markPaid(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam(required = false) String utrNumber,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) MultipartFile invoiceFile) {
        PayoutResponse resp = payoutService.markPaid(currentUser.getOrgId(), id, currentUser.getId(), utrNumber, paymentMethod, invoiceFile);
        return ResponseEntity.ok(ApiResponse.success(resp, "Payout marked PAID, transaction created, UTR: " + utrNumber));
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("@permEval.hasPermission('VENDOR_PAYOUT_MANAGE')")
    public ResponseEntity<ApiResponse<PayoutResponse>> failPayout(@CurrentUser UserPrincipal currentUser,
                                                                 @PathVariable Long id,
                                                                 @RequestParam(required = false) String reason) {
        PayoutResponse resp = payoutService.failPayout(currentUser.getOrgId(), id, reason);
        return ResponseEntity.ok(ApiResponse.success(resp, "Payout marked FAILED"));
    }
}
