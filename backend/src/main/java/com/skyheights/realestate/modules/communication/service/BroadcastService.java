package com.skyheights.realestate.modules.communication.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.communication.dto.BroadcastCreateRequest;
import com.skyheights.realestate.modules.communication.dto.BroadcastResponse;
import com.skyheights.realestate.modules.communication.entity.AnnouncementRecipient;
import com.skyheights.realestate.modules.communication.entity.BroadcastAnnouncement;
import com.skyheights.realestate.modules.communication.repository.AnnouncementRecipientRepository;
import com.skyheights.realestate.modules.communication.repository.BroadcastAnnouncementRepository;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.portfolio.repository.PropertyRepository;
import com.skyheights.realestate.modules.portfolio.repository.UnitRepository;
import com.skyheights.realestate.modules.tenant.entity.TenantProfile;
import com.skyheights.realestate.modules.tenant.repository.TenantProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastService {

    private final BroadcastAnnouncementRepository broadcastRepository;
    private final AnnouncementRecipientRepository recipientRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final AppUserRepository appUserRepository;
    private final TenantProfileRepository tenantRepository;
    private final S3Service s3Service;
    private final NotificationService notificationService;

    @Transactional
    public BroadcastResponse createBroadcast(Long orgId, Long createdByUserId, BroadcastCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        AppUser creator = appUserRepository.findByIdAndIsDeletedFalse(createdByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator user not found"));

        Property property = null;
        if (request.getPropertyId() != null) {
            property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        }

        Unit unit = null;
        if (request.getUnitId() != null) {
            unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUnitId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            if (property != null && !unit.getProperty().getId().equals(property.getId())) {
                throw new RuntimeException("Unit does not belong to property");
            }
        }

        BroadcastAnnouncement announcement = BroadcastAnnouncement.builder()
                .organization(org)
                .property(property)
                .unit(unit)
                .title(request.getTitle())
                .message(request.getMessage())
                .priority(request.getPriority() != null ? request.getPriority() : com.skyheights.realestate.modules.communication.enums.BroadcastPriority.MEDIUM)
                .category(request.getCategory())
                .createdByUser(creator)
                .expiresAt(request.getExpiresAt())
                .isActive(true)
                .attachmentS3Key(request.getAttachmentS3Key())
                .actionRequired(request.getActionRequired() != null ? request.getActionRequired() : false)
                .actionLabel(request.getActionLabel())
                .sendPush(request.getSendPush() != null ? request.getSendPush() : true)
                .sendSms(request.getSendSms() != null ? request.getSendSms() : false)
                .sendWhatsapp(request.getSendWhatsapp() != null ? request.getSendWhatsapp() : true)
                .sendEmail(request.getSendEmail() != null ? request.getSendEmail() : false)
                .build();

        announcement = broadcastRepository.save(announcement);

        // Determine recipients
        List<AppUser> recipients = determineRecipients(orgId, property, unit, request.getRecipientUserIds());

        for (AppUser recipient : recipients) {
            AnnouncementRecipient rec = new AnnouncementRecipient();
            rec.setAnnouncement(announcement);
            rec.setRecipientUser(recipient);
            rec.setStatus("SENT");
            rec.setCreatedAt(Instant.now());
            recipientRepository.save(rec);
        }

        // Send via notification service based on flags
        try {
            for (AppUser recipient : recipients) {
                if (Boolean.TRUE.equals(request.getSendPush())) {
                    // Push via NotificationService
                    notificationService.sendNotification(orgId, com.skyheights.realestate.modules.communication.dto.SendNotificationRequest.builder()
                            .channel(com.skyheights.realestate.modules.communication.enums.NotificationChannel.PUSH)
                            .recipientContact(recipient.getEmail()) // push uses email or device token? For demo email
                            .recipientUserId(recipient.getId())
                            .recipientType("USER")
                            .subject(request.getTitle())
                            .body(request.getMessage())
                            .relatedEntityType("BROADCAST")
                            .relatedEntityId(announcement.getId())
                            .build());
                }
                if (Boolean.TRUE.equals(request.getSendWhatsapp()) && recipient.getPhone() != null) {
                    notificationService.sendNotification(orgId, com.skyheights.realestate.modules.communication.dto.SendNotificationRequest.builder()
                            .channel(com.skyheights.realestate.modules.communication.enums.NotificationChannel.WHATSAPP)
                            .recipientContact(recipient.getPhone())
                            .recipientUserId(recipient.getId())
                            .recipientType("USER")
                            .subject(request.getTitle())
                            .body(request.getMessage())
                            .relatedEntityType("BROADCAST")
                            .relatedEntityId(announcement.getId())
                            .build());
                }
                if (Boolean.TRUE.equals(request.getSendEmail())) {
                    notificationService.sendNotification(orgId, com.skyheights.realestate.modules.communication.dto.SendNotificationRequest.builder()
                            .channel(com.skyheights.realestate.modules.communication.enums.NotificationChannel.EMAIL)
                            .recipientContact(recipient.getEmail())
                            .recipientUserId(recipient.getId())
                            .recipientType("USER")
                            .subject(request.getTitle())
                            .body(request.getMessage())
                            .relatedEntityType("BROADCAST")
                            .relatedEntityId(announcement.getId())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send broadcast notifications: {}", e.getMessage());
        }

        log.info("Created broadcast {} '{}' org {} property {} recipients {}",
                announcement.getId(), announcement.getTitle(), orgId,
                property != null ? property.getId() : "org-wide", recipients.size());

        return toResponse(announcement);
    }

    @Transactional(readOnly = true)
    public Page<BroadcastResponse> searchBroadcasts(Long orgId, Long propertyId, Boolean isActive, Pageable pageable) {
        Page<BroadcastAnnouncement> page = broadcastRepository.search(orgId, propertyId, isActive, Instant.now(), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BroadcastResponse getBroadcast(Long orgId, Long id) {
        BroadcastAnnouncement announcement = broadcastRepository.findById(id)
                .filter(b -> b.getOrganization().getId().equals(orgId) && !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Broadcast not found"));
        return toResponse(announcement);
    }

    @Transactional
    public BroadcastResponse uploadAttachment(Long orgId, Long id, MultipartFile file) {
        BroadcastAnnouncement announcement = broadcastRepository.findById(id)
                .filter(b -> b.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Broadcast not found"));

        if (file == null || file.isEmpty()) throw new RuntimeException("File required");

        try {
            String key = s3Service.generateKey(orgId, "broadcasts/" + id, file.getOriginalFilename());
            String s3Key = s3Service.uploadFile(key, file.getInputStream(), file.getSize(), file.getContentType());
            announcement.setAttachmentS3Key(s3Key);
            broadcastRepository.save(announcement);
            log.info("Uploaded broadcast attachment {} for broadcast {} org {}", s3Key, id, orgId);
            return toResponse(announcement);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload attachment: " + e.getMessage());
        }
    }

    @Transactional
    public BroadcastResponse markRead(Long broadcastId, Long userId) {
        AnnouncementRecipient rec = recipientRepository.findByAnnouncementIdAndRecipientUserId(broadcastId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient entry not found"));

        rec.setReadAt(Instant.now());
        rec.setStatus("READ");
        recipientRepository.save(rec);

        BroadcastAnnouncement announcement = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new ResourceNotFoundException("Broadcast not found"));

        return toResponse(announcement);
    }

    private List<AppUser> determineRecipients(Long orgId, Property property, Unit unit, List<Long> specificUserIds) {
        if (specificUserIds != null && !specificUserIds.isEmpty()) {
            return appUserRepository.findAllById(specificUserIds).stream()
                    .filter(u -> {
                        Long userOrgId = u.getOrganization() != null ? u.getOrganization().getId() : u.getOrgId();
                        return userOrgId != null && userOrgId.equals(orgId) && !Boolean.TRUE.equals(u.getIsDeleted());
                    })
                    .collect(Collectors.toList());
        }

        // If unit specified, only tenant of that unit
        if (unit != null) {
            List<TenantProfile> tenants = tenantRepository.findByUnitIdAndIsDeletedFalse(unit.getId());
            return tenants.stream()
                    .map(TenantProfile::getUser)
                    .filter(u -> u != null && !Boolean.TRUE.equals(u.getIsDeleted()))
                    .collect(Collectors.toList());
        }

        // If property specified, all tenants in property
        if (property != null) {
            // Find all units in property, then tenants
            var units = unitRepository.findByPropertyIdAndIsDeletedFalse(property.getId());
            List<AppUser> users = new ArrayList<>();
            for (var u : units) {
                var tenants = tenantRepository.findByUnitIdAndIsDeletedFalse(u.getId());
                for (var t : tenants) {
                    if (t.getUser() != null && !Boolean.TRUE.equals(t.getUser().getIsDeleted())) {
                        users.add(t.getUser());
                    }
                }
            }
            return users.stream().distinct().collect(Collectors.toList());
        }

        // Org-wide: all active users in org (or all tenants? For demo, all users)
        return appUserRepository.findByOrgIdAndIsDeletedFalse(orgId);
    }

    private BroadcastResponse toResponse(BroadcastAnnouncement b) {
        List<AnnouncementRecipient> recipients = recipientRepository.findByAnnouncementId(b.getId());

        List<BroadcastResponse.RecipientStatus> recipientStatuses = recipients.stream().map(r -> BroadcastResponse.RecipientStatus.builder()
                .id(r.getId())
                .recipientUserId(r.getRecipientUser() != null ? r.getRecipientUser().getId() : null)
                .recipientName(r.getRecipientUser() != null ? r.getRecipientUser().getFullName() : null)
                .recipientContact(r.getRecipientUser() != null ? r.getRecipientUser().getEmail() : null)
                .status(r.getStatus())
                .readAt(r.getReadAt())
                .build()).collect(Collectors.toList());

        long delivered = recipients.stream().filter(r -> !"SENT".equals(r.getStatus())).count();
        long read = recipients.stream().filter(r -> "READ".equals(r.getStatus())).count();

        String presigned = null;
        try {
            if (b.getAttachmentS3Key() != null) presigned = s3Service.generatePresignedUrl(b.getAttachmentS3Key(), Duration.ofMinutes(30));
        } catch (Exception ignored) {}

        return BroadcastResponse.builder()
                .id(b.getId()).uuid(b.getUuid())
                .orgId(b.getOrganization() != null ? b.getOrganization().getId() : null)
                .propertyId(b.getProperty() != null ? b.getProperty().getId() : null)
                .propertyName(b.getProperty() != null ? b.getProperty().getName() : null)
                .unitId(b.getUnit() != null ? b.getUnit().getId() : null)
                .unitNumber(b.getUnit() != null ? b.getUnit().getUnitNumber() : null)
                .title(b.getTitle()).message(b.getMessage()).priority(b.getPriority()).category(b.getCategory())
                .createdByUserId(b.getCreatedByUser() != null ? b.getCreatedByUser().getId() : null)
                .createdByName(b.getCreatedByUser() != null ? b.getCreatedByUser().getFullName() : null)
                .expiresAt(b.getExpiresAt()).isActive(b.getIsActive())
                .attachmentS3Key(b.getAttachmentS3Key()).attachmentPresignedUrl(presigned)
                .actionRequired(b.getActionRequired()).actionLabel(b.getActionLabel())
                .sendPush(b.getSendPush()).sendSms(b.getSendSms()).sendWhatsapp(b.getSendWhatsapp()).sendEmail(b.getSendEmail())
                .createdAt(b.getCreatedAt())
                .totalRecipients(recipients.size()).deliveredCount(delivered).readCount(read)
                .recipients(recipientStatuses)
                .build();
    }
}
