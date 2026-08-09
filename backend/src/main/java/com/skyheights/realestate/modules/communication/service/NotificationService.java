package com.skyheights.realestate.modules.communication.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.communication.dto.NotificationLogResponse;
import com.skyheights.realestate.modules.communication.dto.SendNotificationRequest;
import com.skyheights.realestate.modules.communication.entity.NotificationLog;
import com.skyheights.realestate.modules.communication.entity.NotificationTemplate;
import com.skyheights.realestate.modules.communication.enums.NotificationChannel;
import com.skyheights.realestate.modules.communication.enums.NotificationStatus;
import com.skyheights.realestate.modules.communication.repository.NotificationLogRepository;
import com.skyheights.realestate.modules.communication.repository.NotificationTemplateRepository;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationLogRepository logRepository;
    private final NotificationTemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_KEY = "notification:queue";

    @Transactional
    public NotificationLogResponse sendNotification(Long orgId, SendNotificationRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        NotificationTemplate template = null;
        String subjectRendered = request.getSubject();
        String bodyRendered = request.getBody();

        if (request.getTemplateId() != null) {
            template = templateRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getTemplateId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        } else if (request.getTemplateCode() != null) {
            template = templateRepository.findByOrganizationIdAndCodeAndIsDeletedFalse(orgId, request.getTemplateCode().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found with code " + request.getTemplateCode()));
        }

        if (template != null) {
            Map<String, String> vars = request.getVariables() != null ? request.getVariables() : Map.of();
            subjectRendered = renderTemplate(template.getSubject(), vars);
            bodyRendered = renderTemplate(template.getBody(), vars);
        }

        if (bodyRendered == null || bodyRendered.isBlank()) {
            throw new RuntimeException("Body is required (either template or direct body)");
        }

        AppUser recipientUser = null;
        if (request.getRecipientUserId() != null) {
            recipientUser = appUserRepository.findByIdAndIsDeletedFalse(request.getRecipientUserId())
                    .orElse(null);
        }

        NotificationLog logEntry = NotificationLog.builder()
                .organization(org)
                .template(template)
                .channel(request.getChannel())
                .recipientType(request.getRecipientType() != null ? request.getRecipientType() : "USER")
                .recipient(recipientUser)
                .recipientContact(request.getRecipientContact())
                .subjectRendered(subjectRendered)
                .bodyRendered(bodyRendered)
                .status(NotificationStatus.QUEUED)
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .retryCount(0)
                .build();

        logEntry = logRepository.save(logEntry);

        // Push to Redis queue for async sending
        try {
            Map<String, Object> queuePayload = Map.of(
                    "logId", logEntry.getId(),
                    "orgId", orgId,
                    "channel", request.getChannel().name(),
                    "recipientContact", request.getRecipientContact(),
                    "subject", subjectRendered != null ? subjectRendered : "",
                    "body", bodyRendered,
                    "attempt", 0
            );
            redisTemplate.opsForList().leftPush(QUEUE_KEY, objectMapper.writeValueAsString(queuePayload));
            log.info("Queued notification {} for channel {} to {} org {}", logEntry.getId(), request.getChannel(), request.getRecipientContact(), orgId);
        } catch (Exception e) {
            log.warn("Failed to push to Redis queue, will be sent synchronously fallback: {}", e.getMessage());
            // Fallback to direct send mock
            directSendMock(logEntry);
        }

        return toResponse(logEntry);
    }

    @Transactional
    public NotificationLogResponse directSendMock(NotificationLog logEntry) {
        // Mock provider sending based on channel
        try {
            NotificationChannel channel = logEntry.getChannel();
            String contact = logEntry.getRecipientContact();

            // Simulate provider call latency and success/failure
            // For demo, 95% success rate
            boolean success = Math.random() > 0.05;

            if (success) {
                logEntry.setStatus(NotificationStatus.SENT);
                logEntry.setSentAt(Instant.now());
                logEntry.setProviderMessageId("mock-" + channel.name().toLowerCase() + "-" + UUID.randomUUID());
                log.debug("Mock sent {} notification {} to {} via {}", logEntry.getId(), channel, contact, channel);

                // Simulate delivered after short delay? For now mark SENT, delivery tracking would be webhook
                // For email/sms we could mark DELIVERED immediately in mock
                if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.SMS) {
                    logEntry.setStatus(NotificationStatus.DELIVERED);
                    logEntry.setDeliveredAt(Instant.now().plusSeconds(2));
                }
            } else {
                logEntry.setStatus(NotificationStatus.FAILED);
                logEntry.setFailureReason("Mock provider failure - simulated 5% failure rate");
                logEntry.setRetryCount(logEntry.getRetryCount() + 1);
                logEntry.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(5 * logEntry.getRetryCount())));
                log.warn("Mock failed to send notification {} to {} via {}", logEntry.getId(), contact, channel);
            }

            logRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed direct send mock for log {}", logEntry.getId(), e);
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setFailureReason(e.getMessage());
            logEntry.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(5)));
            logRepository.save(logEntry);
        }

        return toResponse(logEntry);
    }

    @Transactional(readOnly = true)
    public Page<NotificationLogResponse> searchLogs(Long orgId, NotificationChannel channel, com.skyheights.realestate.modules.communication.enums.NotificationStatus status,
                                                    String recipientType, String relatedEntityType, Pageable pageable) {
        Page<NotificationLog> page = logRepository.search(orgId, channel, status, recipientType, relatedEntityType, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationLogResponse getLog(Long orgId, Long id) {
        NotificationLog logEntry = logRepository.findById(id)
                .filter(l -> l.getOrganization().getId().equals(orgId) && !Boolean.TRUE.equals(l.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Notification log not found"));
        return toResponse(logEntry);
    }

    @Transactional
    public NotificationLogResponse retryFailed(Long orgId, Long id) {
        NotificationLog logEntry = logRepository.findById(id)
                .filter(l -> l.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Log not found"));

        if (logEntry.getStatus() != NotificationStatus.FAILED && logEntry.getStatus() != NotificationStatus.QUEUED) {
            throw new RuntimeException("Only FAILED or QUEUED logs can be retried");
        }

        logEntry.setStatus(NotificationStatus.QUEUED);
        logEntry.setRetryCount(logEntry.getRetryCount() + 1);
        logEntry.setNextRetryAt(null);
        logEntry.setFailureReason(null);
        logRepository.save(logEntry);

        // Re-queue
        try {
            Map<String, Object> payload = Map.of(
                    "logId", logEntry.getId(),
                    "orgId", orgId,
                    "channel", logEntry.getChannel().name(),
                    "recipientContact", logEntry.getRecipientContact(),
                    "subject", logEntry.getSubjectRendered() != null ? logEntry.getSubjectRendered() : "",
                    "body", logEntry.getBodyRendered(),
                    "attempt", logEntry.getRetryCount()
            );
            redisTemplate.opsForList().leftPush(QUEUE_KEY, objectMapper.writeValueAsString(payload));
            log.info("Retried notification {} org {} attempt {}", id, orgId, logEntry.getRetryCount());
        } catch (Exception e) {
            log.warn("Failed to re-queue for retry: {}", e.getMessage());
        }

        return toResponse(logEntry);
    }

    public String renderTemplate(String template, Map<String, String> variables) {
        if (template == null) return null;
        String rendered = template;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder1 = "{{" + entry.getKey() + "}}";
                String placeholder2 = "{{ " + entry.getKey() + " }}";
                rendered = rendered.replace(placeholder1, entry.getValue() != null ? entry.getValue() : "");
                rendered = rendered.replace(placeholder2, entry.getValue() != null ? entry.getValue() : "");
            }
        }
        // Remove any unreplaced placeholders
        rendered = rendered.replaceAll("\\{\\{[^}]+\\}\\}", "");
        return rendered;
    }

    private NotificationLogResponse toResponse(NotificationLog l) {
        return NotificationLogResponse.builder()
                .id(l.getId()).uuid(l.getUuid())
                .orgId(l.getOrganization() != null ? l.getOrganization().getId() : null)
                .templateId(l.getTemplate() != null ? l.getTemplate().getId() : null)
                .templateCode(l.getTemplate() != null ? l.getTemplate().getCode() : null)
                .channel(l.getChannel())
                .recipientType(l.getRecipientType())
                .recipientId(l.getRecipient() != null ? l.getRecipient().getId() : null)
                .recipientContact(l.getRecipientContact())
                .subjectRendered(l.getSubjectRendered())
                .bodyRendered(l.getBodyRendered())
                .status(l.getStatus())
                .providerMessageId(l.getProviderMessageId())
                .sentAt(l.getSentAt())
                .deliveredAt(l.getDeliveredAt())
                .failureReason(l.getFailureReason())
                .relatedEntityType(l.getRelatedEntityType())
                .relatedEntityId(l.getRelatedEntityId())
                .retryCount(l.getRetryCount())
                .nextRetryAt(l.getNextRetryAt())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
