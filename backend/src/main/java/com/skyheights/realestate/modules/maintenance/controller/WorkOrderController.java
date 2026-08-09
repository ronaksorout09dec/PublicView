package com.skyheights.realestate.modules.maintenance.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.maintenance.dto.WorkOrderResponse;
import com.skyheights.realestate.modules.maintenance.enums.WorkOrderStatus;
import com.skyheights.realestate.modules.maintenance.service.WorkOrderService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> createWorkOrder(@CurrentUser UserPrincipal currentUser,
                                                                          @RequestParam Long ticketId,
                                                                          @RequestParam Long bidId,
                                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate) {
        WorkOrderResponse resp = workOrderService.createWorkOrder(currentUser.getOrgId(), ticketId, bidId, currentUser.getId(), scheduledDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Work order created, ticket now IN_PROGRESS"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE') or @permEval.hasPermission('VENDOR_BID')")
    public ResponseEntity<ApiResponse<Page<WorkOrderResponse>>> getWorkOrders(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) WorkOrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WorkOrderResponse> page = workOrderService.searchWorkOrders(currentUser.getOrgId(), vendorId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Work orders fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE') or @permEval.hasPermission('VENDOR_BID')")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> getWorkOrder(@CurrentUser UserPrincipal currentUser,
                                                                       @PathVariable Long id) {
        WorkOrderResponse resp = workOrderService.getWorkOrder(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Work order fetched"));
    }

    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE') or @permEval.hasPermission('VENDOR_BID')")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> getByTicket(@CurrentUser UserPrincipal currentUser,
                                                                      @PathVariable Long ticketId) {
        WorkOrderResponse resp = workOrderService.getByTicket(currentUser.getOrgId(), ticketId);
        return ResponseEntity.ok(ApiResponse.success(resp, "Work order fetched by ticket"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE') or @permEval.hasPermission('VENDOR_BID')")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> updateStatus(@CurrentUser UserPrincipal currentUser,
                                                                       @PathVariable Long id,
                                                                       @RequestParam WorkOrderStatus status,
                                                                       @RequestParam(required = false) String completionNotes) {
        WorkOrderResponse resp = workOrderService.updateStatus(currentUser.getOrgId(), id, status, completionNotes);
        return ResponseEntity.ok(ApiResponse.success(resp, "Work order status updated to " + status));
    }

    @PostMapping(value = "/{id}/invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE') or @permEval.hasPermission('VENDOR_BID')")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> uploadInvoice(@CurrentUser UserPrincipal currentUser,
                                                                        @PathVariable Long id,
                                                                        @RequestParam("file") MultipartFile file) {
        WorkOrderResponse resp = workOrderService.uploadInvoice(currentUser.getOrgId(), id, file);
        return ResponseEntity.ok(ApiResponse.success(resp, "Work order invoice uploaded to S3"));
    }
}
