package com.skyheights.realestate.modules.communication.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.communication.dto.BroadcastCreateRequest;
import com.skyheights.realestate.modules.communication.dto.BroadcastResponse;
import com.skyheights.realestate.modules.communication.service.BroadcastService;
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
@RequestMapping("/api/broadcasts")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastService broadcastService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_SEND') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<BroadcastResponse>> createBroadcast(@CurrentUser UserPrincipal currentUser,
                                                                          @Valid @RequestBody BroadcastCreateRequest request) {
        BroadcastResponse resp = broadcastService.createBroadcast(currentUser.getOrgId(), currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Broadcast announcement created, e.g 'Water supply cut tomorrow'"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_SEND') or @permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<Page<BroadcastResponse>>> getBroadcasts(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<BroadcastResponse> page = broadcastService.searchBroadcasts(currentUser.getOrgId(), propertyId, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Broadcasts fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_SEND') or @permEval.hasPermission('TENANT_READ')")
    public ResponseEntity<ApiResponse<BroadcastResponse>> getBroadcast(@CurrentUser UserPrincipal currentUser,
                                                                       @PathVariable Long id) {
        BroadcastResponse resp = broadcastService.getBroadcast(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Broadcast fetched"));
    }

    @PostMapping(value = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_SEND')")
    public ResponseEntity<ApiResponse<BroadcastResponse>> uploadAttachment(@CurrentUser UserPrincipal currentUser,
                                                                           @PathVariable Long id,
                                                                           @RequestParam("file") MultipartFile file) {
        BroadcastResponse resp = broadcastService.uploadAttachment(currentUser.getOrgId(), id, file);
        return ResponseEntity.ok(ApiResponse.success(resp, "Attachment uploaded to S3"));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("@permEval.hasPermission('TENANT_READ') or @permEval.hasPermission('COMMUNICATION_SEND')")
    public ResponseEntity<ApiResponse<BroadcastResponse>> markRead(@CurrentUser UserPrincipal currentUser,
                                                                   @PathVariable Long id) {
        BroadcastResponse resp = broadcastService.markRead(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(resp, "Broadcast marked as read"));
    }
}
