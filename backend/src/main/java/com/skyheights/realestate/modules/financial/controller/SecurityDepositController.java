package com.skyheights.realestate.modules.financial.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.financial.dto.DepositLedgerCreateRequest;
import com.skyheights.realestate.modules.financial.dto.SecurityDepositResponse;
import com.skyheights.realestate.modules.financial.service.SecurityDepositService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class SecurityDepositController {

    private final SecurityDepositService depositService;

    @PostMapping("/lease/{leaseId}")
    @PreAuthorize("@permEval.hasPermission('DEPOSIT_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<SecurityDepositResponse>> createDepositForLease(@CurrentUser UserPrincipal currentUser,
                                                                                     @PathVariable Long leaseId) {
        SecurityDepositResponse resp = depositService.createDepositForLease(currentUser.getOrgId(), leaseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Security deposit created"));
    }

    @GetMapping("/lease/{leaseId}")
    @PreAuthorize("@permEval.hasPermission('DEPOSIT_MANAGE') or @permEval.hasPermission('LEASE_MANAGE')")
    public ResponseEntity<ApiResponse<SecurityDepositResponse>> getByLease(@CurrentUser UserPrincipal currentUser,
                                                                          @PathVariable Long leaseId) {
        SecurityDepositResponse resp = depositService.getDepositByLease(currentUser.getOrgId(), leaseId);
        return ResponseEntity.ok(ApiResponse.success(resp, "Deposit fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('DEPOSIT_MANAGE')")
    public ResponseEntity<ApiResponse<SecurityDepositResponse>> getDeposit(@CurrentUser UserPrincipal currentUser,
                                                                           @PathVariable Long id) {
        SecurityDepositResponse resp = depositService.getDeposit(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Deposit fetched"));
    }

    @PostMapping("/ledger")
    @PreAuthorize("@permEval.hasPermission('DEPOSIT_MANAGE')")
    public ResponseEntity<ApiResponse<SecurityDepositResponse>> addLedgerEntry(@CurrentUser UserPrincipal currentUser,
                                                                               @Valid @RequestBody DepositLedgerCreateRequest request) {
        SecurityDepositResponse resp = depositService.addLedgerEntry(currentUser.getOrgId(), currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Deposit ledger entry added"));
    }
}
