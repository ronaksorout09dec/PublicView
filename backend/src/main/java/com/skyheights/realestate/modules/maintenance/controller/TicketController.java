package com.skyheights.realestate.modules.maintenance.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.maintenance.dto.TicketCreateRequest;
import com.skyheights.realestate.modules.maintenance.dto.TicketResponse;
import com.skyheights.realestate.modules.maintenance.enums.TicketPriority;
import com.skyheights.realestate.modules.maintenance.enums.TicketStatus;
import com.skyheights.realestate.modules.maintenance.service.TicketService;
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
@RequestMapping("/api/maintenance/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('TICKET_CREATE') or @permEval.hasPermission('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(@CurrentUser UserPrincipal currentUser,
                                                                    @Valid @RequestBody TicketCreateRequest request) {
        TicketResponse resp = ticketService.createTicket(currentUser.getOrgId(), currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Ticket raised with photo/video support"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE') or @permEval.hasPermission('TICKET_CREATE')")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> getTickets(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long assignedVendorId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TicketResponse> page = ticketService.searchTickets(currentUser.getOrgId(), propertyId, unitId, tenantId, status, priority, category, assignedVendorId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Tickets fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE') or @permEval.hasPermission('TICKET_CREATE')")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@CurrentUser UserPrincipal currentUser,
                                                                 @PathVariable Long id) {
        TicketResponse resp = ticketService.getTicket(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Ticket fetched"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(@CurrentUser UserPrincipal currentUser,
                                                                    @PathVariable Long id,
                                                                    @RequestParam TicketStatus status) {
        TicketResponse resp = ticketService.updateTicketStatus(currentUser.getOrgId(), id, status);
        return ResponseEntity.ok(ApiResponse.success(resp, "Ticket status updated"));
    }

    @PostMapping("/{id}/broadcast")
    @PreAuthorize("@permEval.hasPermission('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<TicketResponse>> broadcastTicket(@CurrentUser UserPrincipal currentUser,
                                                                       @PathVariable Long id) {
        TicketResponse resp = ticketService.broadcastTicket(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Ticket broadcasted to vendors, now BIDDING"));
    }

    @PostMapping(value = "/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permEval.hasPermission('TICKET_CREATE') or @permEval.hasPermission('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<TicketResponse>> addMedia(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) List<String> mediaTypes,
            @RequestParam(required = false) List<String> captions) {
        TicketResponse resp = ticketService.addMedia(currentUser.getOrgId(), id, files, mediaTypes, captions, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(resp, "Media uploaded to S3"));
    }

    @PostMapping("/{id}/rate")
    @PreAuthorize("@permEval.hasPermission('TICKET_CREATE') or @permEval.hasPermission('TICKET_MANAGE')")
    public ResponseEntity<ApiResponse<TicketResponse>> rateTicket(@CurrentUser UserPrincipal currentUser,
                                                                  @PathVariable Long id,
                                                                  @RequestParam int rating,
                                                                  @RequestParam(required = false) String feedback) {
        TicketResponse resp = ticketService.rateTicket(currentUser.getOrgId(), id, rating, feedback);
        return ResponseEntity.ok(ApiResponse.success(resp, "Ticket rated, vendor rating updated"));
    }
}
