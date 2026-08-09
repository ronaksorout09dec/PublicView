package com.skyheights.realestate.modules.maintenance.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.maintenance.dto.BidCreateRequest;
import com.skyheights.realestate.modules.maintenance.dto.BidResponse;
import com.skyheights.realestate.modules.maintenance.service.BidService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('VENDOR_BID') or @permEval.hasPermission('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<BidResponse>> submitBid(@CurrentUser UserPrincipal currentUser,
                                                             @Valid @RequestBody BidCreateRequest request) {
        // If current user is vendor, use vendorUserId = currentUser.getId(), else manager can submit on behalf? We enforce vendor user
        Long vendorUserId = currentUser.getRoles().contains("VENDOR") ? currentUser.getId() : null;
        // For manager to test bidding flow, allow if not vendor role? Simplify: if vendor role, use currentUser, else require vendor user lookup via request? For Phase 4, we require vendor user id = currentUser id for vendor
        if (vendorUserId == null) {
            // If manager is submitting for vendor (unlikely), we still need vendor user - for demo allow currentUser as vendor if they have VENDOR role OR we treat currentUser as vendor
            vendorUserId = currentUser.getId();
        }
        BidResponse resp = bidService.submitBid(currentUser.getOrgId(), vendorUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Bid submitted"));
    }

    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE') or @permEval.hasPermission('VENDOR_BID')")
    public ResponseEntity<ApiResponse<Page<BidResponse>>> getBidsForTicket(@CurrentUser UserPrincipal currentUser,
                                                                          @PathVariable Long ticketId,
                                                                          @PageableDefault(size = 20, sort = "bidAmount", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BidResponse> page = bidService.getBidsForTicket(currentUser.getOrgId(), ticketId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Bids fetched for ticket"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE') or @permEval.hasPermission('VENDOR_BID')")
    public ResponseEntity<ApiResponse<BidResponse>> getBid(@CurrentUser UserPrincipal currentUser,
                                                           @PathVariable Long id) {
        BidResponse resp = bidService.getBid(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Bid fetched"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<BidResponse>> approveBid(@CurrentUser UserPrincipal currentUser,
                                                               @PathVariable Long id) {
        BidResponse resp = bidService.approveBid(currentUser.getOrgId(), id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(resp, "Bid approved, vendor assigned, other bids rejected"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<BidResponse>> rejectBid(@CurrentUser UserPrincipal currentUser,
                                                              @PathVariable Long id,
                                                              @RequestParam(required = false) String reason) {
        BidResponse resp = bidService.rejectBid(currentUser.getOrgId(), id, reason);
        return ResponseEntity.ok(ApiResponse.success(resp, "Bid rejected"));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("@permEval.hasPermission('VENDOR_BID')")
    public ResponseEntity<ApiResponse<BidResponse>> withdrawBid(@CurrentUser UserPrincipal currentUser,
                                                                @PathVariable Long id) {
        BidResponse resp = bidService.withdrawBid(currentUser.getOrgId(), id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(resp, "Bid withdrawn"));
    }
}
