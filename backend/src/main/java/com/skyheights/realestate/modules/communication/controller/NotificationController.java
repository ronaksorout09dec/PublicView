package com.skyheights.realestate.modules.communication.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.communication.dto.NotificationLogResponse;
import com.skyheights.realestate.modules.communication.dto.SendNotificationRequest;
import com.skyheights.realestate.modules.communication.enums.NotificationChannel;
import com.skyheights.realestate.modules.communication.enums.NotificationStatus;
import com.skyheights.realestate.modules.communication.service.NotificationService;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_SEND') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<NotificationLogResponse>> sendNotification(@CurrentUser UserPrincipal currentUser,
                                                                                @Valid @RequestBody SendNotificationRequest request) {
        NotificationLogResponse resp = notificationService.sendNotification(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Notification queued for " + request.getChannel()));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_SEND') or @permEval.hasPermission('COMMUNICATION_TEMPLATE_MANAGE')")
    public ResponseEntity<ApiResponse<Page<NotificationLogResponse>>> getLogs(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String recipientType,
            @RequestParam(required = false) String relatedEntityType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationLogResponse> page = notificationService.searchLogs(currentUser.getOrgId(), channel, status, recipientType, relatedEntityType, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Notification logs fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_SEND')")
    public ResponseEntity<ApiResponse<NotificationLogResponse>> getLog(@CurrentUser UserPrincipal currentUser,
                                                                      @PathVariable Long id) {
        NotificationLogResponse resp = notificationService.getLog(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Notification log fetched"));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("@permEval.hasPermission('COMMUNICATION_SEND')")
    public ResponseEntity<ApiResponse<NotificationLogResponse>> retryFailed(@CurrentUser UserPrincipal currentUser,
                                                                           @PathVariable Long id) {
        NotificationLogResponse resp = notificationService.retryFailed(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Notification re-queued for retry"));
    }
}
