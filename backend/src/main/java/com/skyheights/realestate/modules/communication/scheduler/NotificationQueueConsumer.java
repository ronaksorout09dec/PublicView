package com.skyheights.realestate.modules.communication.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyheights.realestate.modules.communication.entity.NotificationLog;
import com.skyheights.realestate.modules.communication.enums.NotificationStatus;
import com.skyheights.realestate.modules.communication.repository.NotificationLogRepository;
import com.skyheights.realestate.modules.communication.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Consumes notification queue from Redis List "notification:queue"
 * Pushed by NotificationService.sendNotification
 * Polls every 10 seconds, processes up to 20 messages per poll
 * Mock providers: EMAIL, SMS, WHATSAPP, PUSH, IN_APP
 * Retry logic: FAILED logs get nextRetryAt + 5min * retryCount
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationQueueConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationLogRepository logRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_KEY = "notification:queue";

    @Scheduled(fixedDelay = 10000) // every 10 sec
    @Transactional
    public void consumeQueue() {
        try {
            // Try to pop up to 20 messages per cycle
            for (int i = 0; i < 20; i++) {
                Object raw = redisTemplate.opsForList().rightPop(QUEUE_KEY);
                if (raw == null) break;

                try {
                    String json = raw instanceof String ? (String) raw : objectMapper.writeValueAsString(raw);
                    Map<String, Object> payload = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

                    Long logId = ((Number) payload.get("logId")).longValue();
                    String channel = (String) payload.get("channel");
                    String recipientContact = (String) payload.get("recipientContact");

                    NotificationLog logEntry = logRepository.findById(logId).orElse(null);
                    if (logEntry == null) {
                        log.warn("Notification log {} not found for queued message, skipping", logId);
                        continue;
                    }

                    if (logEntry.getStatus() != NotificationStatus.QUEUED) {
                        log.debug("Log {} not in QUEUED status ({}), skipping", logId, logEntry.getStatus());
                        continue;
                    }

                    // Mock send via directSendMock
                    notificationService.directSendMock(logEntry);
                    log.info("📨 Consumed queue: sent {} notification {} to {} via {}", logId, logEntry.getId(), recipientContact, channel);

                } catch (Exception e) {
                    log.error("Failed to process queued notification: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error consuming notification queue", e);
        }
    }

    @Scheduled(fixedDelay = 60000) // every 1 min, retry failed logs where nextRetryAt <= now
    @Transactional
    public void retryFailedNotifications() {
        try {
            var failedLogs = logRepository.findByStatusAndNextRetryAtBeforeAndIsDeletedFalse(
                    NotificationStatus.FAILED, java.time.Instant.now());

            for (var logEntry : failedLogs) {
                if (logEntry.getRetryCount() >= 5) {
                    log.warn("Notification {} exceeded max retries (5), marking BOUNCED", logEntry.getId());
                    logEntry.setStatus(NotificationStatus.BOUNCED);
                    logRepository.save(logEntry);
                    continue;
                }

                log.info("Retrying failed notification {} attempt {} to {}", logEntry.getId(), logEntry.getRetryCount(), logEntry.getRecipientContact());

                // Re-queue
                Map<String, Object> payload = Map.of(
                        "logId", logEntry.getId(),
                        "orgId", logEntry.getOrganization().getId(),
                        "channel", logEntry.getChannel().name(),
                        "recipientContact", logEntry.getRecipientContact(),
                        "subject", logEntry.getSubjectRendered() != null ? logEntry.getSubjectRendered() : "",
                        "body", logEntry.getBodyRendered(),
                        "attempt", logEntry.getRetryCount()
                );

                String json = objectMapper.writeValueAsString(payload);
                redisTemplate.opsForList().leftPush(QUEUE_KEY, json);

                logEntry.setStatus(NotificationStatus.QUEUED);
                logEntry.setNextRetryAt(null);
                logRepository.save(logEntry);
            }

            if (!failedLogs.isEmpty()) {
                log.info("Retried {} failed notifications", failedLogs.size());
            }
        } catch (Exception e) {
            log.error("Error retrying failed notifications", e);
        }
    }
}
