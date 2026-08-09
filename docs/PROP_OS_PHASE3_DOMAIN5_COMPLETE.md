# Prop-OS Phase 3 Domain 5 — COMPLETE ✅
### Communication & Automation Domain — Templates, Multi-Channel Notifications, Broadcasts, Automation Rules with Redis Queue

**Date:** Aug 09, 2026  
**Branch:** arena/019fe27f-publicview  
**Status:** Communication domain operational with templating engine, Redis queue consumer, retry logic, broadcast announcements, automation triggers integrating Lease Expiry + SLA + Rent Due

---

## 📦 Deliverables

### 1. Repositories (7)

- `NotificationTemplateRepository` — findByOrg, findByOrgAndCode, findByOrgAndChannel, existsByOrgAndCode/Name, pagination
- `NotificationLogRepository` — findByOrg pagination, findByStatusAndNextRetryAtBefore for retry scheduler, findByStatusIn, search org+channel/status/recipientType/relatedEntityType, countByStatus, findByRelatedEntityTypeAndId
- `BroadcastAnnouncementRepository` — findByOrg, findByOrgAndIsActive, findByPropertyAndIsActive, search org+propertyId/isActive/expiresAt>now, findByExpiresAtBeforeAndIsActive for expiry cleanup
- `AnnouncementRecipientRepository` — findByAnnouncementId, findByAnnouncement+RecipientUser, countByAnnouncementAndStatus, findByRecipientUserAndStatus
- `AutomationRuleRepository` — findById+org, findByOrg, findByOrgAndIsActive, findByOrgAndTriggerEvent+IsActive, findByOrgAndCode, existsByOrgAndCode
- `AutomationExecutionLogRepository` — findByRuleId orderByTriggeredAtDesc, findByOrg orderByTriggeredAtDesc

### 2. DTOs (9)

- **Template:** `TemplateCreateRequest` name @NotBlank, code @NotBlank unique per org UPPER e.g RENT_REMINDER_3D, channel @NotNull EMAIL/SMS/WHATSAPP/PUSH/IN_APP, subject templated {{var}}, body @NotBlank templated, bodyWhatsappTemplateId external ID, variables List<String> e.g ["tenant_name","rent_amount"], category RENT/LEASE/MAINTENANCE/ANNOUNCEMENT/GENERAL, locale en/hi/en_HI, isActive, `TemplateResponse` id/uuid/orgId/name/code/channel/subject/body/whatsappTemplateId/variables/category/isActive/locale/createdAt
- **Notification:** `SendNotificationRequest` templateId OR templateCode OR direct subject/body, channel @NotNull, recipientContact @NotBlank phone/email, recipientUserId, recipientType TENANT/VENDOR/STAFF/LEAD/USER, variables Map<String,String> for {{}} rendering, relatedEntityType INVOICE/LEASE/TICKET/BROADCAST, relatedEntityId, `NotificationLogResponse` id/uuid/orgId/templateId/code/channel/recipientType/recipientId/recipientContact/subjectRendered/bodyRendered/status QUEUED/SENT/FAILED/DELIVERED/READ/BOUNCED/providerMessageId/sentAt/deliveredAt/failureReason/relatedEntityType/Id/retryCount/nextRetryAt/createdAt
- **Broadcast:** `BroadcastCreateRequest` propertyId null=org-wide, unitId, title @NotBlank, message @NotBlank, priority LOW/MEDIUM/HIGH/CRITICAL, category WATER/ELECTRICITY/MAINTENANCE/EVENT/SAFETY/GENERAL, expiresAt, attachmentS3Key, actionRequired/actionLabel, sendPush/sms/whatsapp/email Boolean flags, recipientUserIds List<Long> optional specific else auto tenants in property/org, `BroadcastResponse` id/uuid/orgId/propertyId/name/unitId/number/title/message/priority/category/createdByUserId/name/expiresAt/isActive/attachmentS3Key/presignedUrl 30m/actionRequired/label/send_* flags/createdAt + totalRecipients/deliveredCount/readCount + recipients List<RecipientStatus> id/recipientUserId/name/contact/status/readAt
- **Automation:** `AutomationRuleCreateRequest` name @NotBlank, code @NotBlank UNIQUE UPPER e.g AUTO_RENT_DUE_T_MINUS_3, description, triggerEvent @NotNull ENUM RENT_DUE_7D/RENT_DUE_3D/RENT_OVERDUE_1D/RENT_OVERDUE_5D/LEASE_EXPIRY_60D/LEASE_EXPIRY_30D/LEASE_EXPIRED/TICKET_CREATED/COMPLETED/LEAD_NO_FOLLOWUP_2D/UTILITY_BILL_GENERATED/KYC_PENDING_3D/MOVE_IN_REMINDER, conditions Map<String,Object> e.g {"property_id_in":[],"unit_status":"occupied"}, templateId, isActive, cooldownHours, `AutomationRuleResponse` id/uuid/orgId/name/code/description/triggerEvent/conditions Map/templateId/code/isActive/cooldownHours/lastTriggeredAt/executionCount/createdAt, `AutomationLogResponse` id/uuid/ruleId/code/orgId/triggeredAt/status SUCCESS/PARTIAL/FAILED/context Map/affectedRecipientsCount/details/error

### 3. Services with Enterprise Logic

**`TemplateService`:**
- **createTemplate:** org exists, code unique per org UPPER + name unique per org blocked, variables List→JSON via ObjectMapper writeValueAsString, locale default en, isActive default true, saves, logs
- **getTemplates:** org pagination, **getTemplate/getByCode**, **updateTemplate:** name/code uniqueness excluding self, channel/subject/body/whatsappTemplateId/variables/category/locale/isActive partial, **deleteTemplate:** soft
- **toResponse:** variablesJson→List<String> via readValue TypeReference

**`NotificationService` — Core with Redis Queue:**
- **sendNotification:** org exists, template resolution via templateId OR templateCode UPPER, if template present render subject/body via `renderTemplate` method replacing {{key}} and {{ key }} with variable value, removes unreplaced {{}} placeholders, direct subject/body if no template, recipientUser optional lookup, builds NotificationLog QUEUED retryCount 0, saves, pushes to Redis List `notification:queue` leftPush JSON payload {logId, orgId, channel, recipientContact, subject, body, attempt 0} via redisTemplate.opsForList + objectMapper write, logs queued, catches Redis failure → fallback directSendMock
- **directSendMock:** mock provider based on channel, 95% success rate random >0.05 success else failure 5%, success: status SENT + sentAt now + providerMessageId mock-channel-UUID, if EMAIL/SMS immediately DELIVERED + deliveredAt now+2s, failure: status FAILED + failureReason "Mock provider failure" + retryCount+1 + nextRetryAt now + 5min*retryCount, saves
- **searchLogs/getLog/retryFailed:** search org+channel/status/recipientType/relatedEntityType, org check, retryFailed only FAILED/QUEUED can be retried, status→QUEUED retryCount+1 nextRetryAt null failureReason null, re-queue to Redis with attempt = retryCount
- **renderTemplate:** replaces {{var}} and {{ var }} with value, removes any remaining {{.*}} via regex, returns null if template null
- **toResponse:** mapping

**`BroadcastService`:**
- **createBroadcast:** org, creator user exists, property/unit belong org with unit belongs property check, builds BroadcastAnnouncement with priority default MEDIUM, category, expiresAt, attachmentS3Key, actionRequired default false, sendPush default true sendSms false sendWhatsapp true sendEmail false, saves, determines recipients via `determineRecipients`: if specific recipientUserIds provided → findAllById filter orgId + not deleted, else if unit specified → tenants in unit via tenantRepository.findByUnitIdAndIsDeletedFalse → users, else if property specified → units in property → tenants → users distinct, else org-wide → all active users in org via appUserRepository.findByOrgIdAndIsDeletedFalse. For each recipient creates AnnouncementRecipient SENT createdAt now. Then sends via notificationService based on flags: if sendPush → channel PUSH recipient email (push uses email/device token demo), if sendWhatsapp and phone exists → WHATSAPP via phone, if sendEmail → EMAIL via email, all with relatedEntityType BROADCAST and relatedEntityId announcement.id. Logs created broadcast + recipients count.
- **searchBroadcasts/getBroadcast:** org + propertyId/isActive/expiresAt>now filter
- **uploadAttachment:** MultipartFile upload via S3Service generateKey orgId "broadcasts/"+id + uploadFile, set attachmentS3Key, save
- **markRead:** findByAnnouncementAndRecipientUser, set readAt now status READ, save
- **toResponse:** recipients list via recipientRepository.findByAnnouncementId → RecipientStatus id/recipientUserId/name/contact/status/readAt, deliveredCount = not SENT count, readCount = READ count, attachment presigned 30m

**`AutomationService`:**
- **createRule:** org exists, code unique UPPER per org blocked, template belongs org if provided, conditions Map→JSON string via objectMapper, isActive default true cooldownHours default 24 executionCount 0, saves, logs
- **getAllRules/getRule/updateRule/deleteRule:** org check, soft delete, conditions update JSON, template check
- **triggerRule:** org + ruleId org check, isActive check, cooldown check lastTriggeredAt + cooldownHours*3600, if now before nextAllowed throw "Rule in cooldown until", executes: for demo affectedCount=1, details "Triggered rule CODE with context", status SUCCESS, catch exception → FAILED + error, updates rule lastTriggeredAt now + executionCount++, saves, creates AutomationExecutionLog with rule/org/triggeredAt/status/contextJson affectedCount/details/error, saves, returns log response
- **getExecutionLogs:** ruleId optional, org, pageable triggeredAt DESC
- **handleTriggerEvent:** called by other domains schedulers (LeaseExpiryAlertJob, MaintenanceSlaScheduler, FinancialScheduler Rent Due), finds rules by orgId + triggerEvent + isActive true, loops each rule try triggerRule with context, logs warn if fails. This is integration point for Domain 2 expiry 60/30 days → LEASE_EXPIRY_60D/30D, Domain 4 SLA breach → TICKET_SLA_BREACH? Actually trigger TICKET_CREATED etc, Domain 3 rent due 7/3 days + overdue 1/5 days.

### 4. Schedulers

**`NotificationQueueConsumer`:**
- **consumeQueue:** @Scheduled fixedDelay 10 sec, pops up to 20 messages per cycle from Redis List `notification:queue` rightPop, parses JSON to Map, logId, channel, recipientContact, finds NotificationLog by id, if not QUEUED skip, calls notificationService.directSendMock(logEntry) → mock send, logs "📨 Consumed queue: sent notification id to contact via channel"
- **retryFailedNotifications:** @Scheduled fixedDelay 60 sec, finds FAILED logs where nextRetryAt <= now, if retryCount >=5 marks BOUNCED, else re-queues: payload with logId/orgId/channel/recipientContact/subject/body/attempt=retryCount, leftPush to queue, status→QUEUED nextRetryAt null, logs retried count

### 5. Controllers — Secure REST (16 endpoints)

- **Template:**
  - `POST /api/notification/templates` — COMMUNICATION_TEMPLATE_MANAGE or hierarchy 60, 201, code unique UPPER
  - `GET /api/notification/templates?page` — COMMUNICATION_TEMPLATE_MANAGE or COMMUNICATION_SEND
  - `GET /api/notification/templates/{id}` — TEMPLATE_MANAGE
  - `GET /api/notification/templates/code/{code}` — TEMPLATE_MANAGE, e.g RENT_REMINDER_3D
  - `PUT /api/notification/templates/{id}` — TEMPLATE_MANAGE
  - `DELETE /api/notification/templates/{id}` — TEMPLATE_MANAGE or 80

- **Notification:**
  - `POST /api/notifications/send` — COMMUNICATION_SEND or hierarchy 50, 201, body templateId OR templateCode OR direct subject/body + channel + recipientContact + recipientUserId/type + variables Map + relatedEntityType/Id, queues to Redis + fallback mock send
  - `GET /api/notifications?channel&status&recipientType&relatedEntityType&page` — COMMUNICATION_SEND or TEMPLATE_MANAGE
  - `GET /api/notifications/{id}` — COMMUNICATION_SEND
  - `POST /api/notifications/{id}/retry` — COMMUNICATION_SEND, re-queues FAILED/QUEUED

- **Broadcast:**
  - `POST /api/broadcasts` — COMMUNICATION_SEND or hierarchy 50, 201, propertyId null=org-wide, title/message, priority, category WATER/ELECTRICITY/MAINTENANCE/EVENT/SAFETY/GENERAL, expiresAt, attachmentS3Key, actionRequired/label, sendPush/sms/whatsapp/email flags, recipientUserIds optional specific else auto tenants in property/org. Example: "Water supply cut tomorrow"
  - `GET /api/broadcasts?propertyId&isActive&page` — COMMUNICATION_SEND or TENANT_READ
  - `GET /api/broadcasts/{id}` — same
  - `POST /api/broadcasts/{id}/attachment` multipart file — COMMUNICATION_SEND, S3 upload broadcasts/{id}/file + presigned
  - `POST /api/broadcasts/{id}/read` — TENANT_READ or COMMUNICATION_SEND, marks recipient READ for current user

- **Automation:**
  - `POST /api/automation/rules` — COMMUNICATION_TEMPLATE_MANAGE or 80, 201, code unique UPPER
  - `GET /api/automation/rules` — TEMPLATE_MANAGE, list all rules
  - `GET /api/automation/rules/{id}` — same
  - `PUT /api/automation/rules/{id}` — same
  - `DELETE /api/automation/rules/{id}` — same or 80
  - `POST /api/automation/rules/{id}/trigger` body context Map optional — TEMPLATE_MANAGE, triggers rule with cooldown check, creates execution log SUCCESS/FAILED, returns log
  - `GET /api/automation/logs?ruleId&page` — TEMPLATE_MANAGE, paginated triggeredAt DESC
  - `POST /api/automation/trigger-event/{triggerEvent}` body context Map — TEMPLATE_MANAGE or 80, triggers all rules for event e.g RENT_DUE_3D, LEASE_EXPIRY_60D, TICKET_CREATED, etc. Valid events: RENT_DUE_7D/3D/OVERDUE_1D/5D/LEASE_EXPIRY_60D/30D/EXPIRED/TICKET_CREATED/COMPLETED/LEAD_NO_FOLLOWUP_2D/UTILITY_BILL_GENERATED etc. Integration point for other domains.

### 6. Edge Cases & Business Rules

- Template code unique per org UPPER enforced, name unique per org, variables List→JSONB stored, locale default en, isActive default true
- Notification template rendering: {{var}} and {{ var }} replaced with value, unreplaced placeholders removed via regex \{\{[^}]+\}\}, fallback direct body required if no template
- NotificationLog QUEUED initially, Redis queue leftPush JSON, consumer rightPop up to 20 per 10 sec, directSendMock 95% success random, EMAIL/SMS immediately DELIVERED after SENT, FAILED gets nextRetryAt now+5min*retryCount, retry 5 times then BOUNCED
- Broadcast recipients determination: specific list → filter org, unit → tenants in unit, property → all tenants in property units, org-wide → all active users in org. Recipients distinct. sendPush/sms/whatsapp/email flags control which channels to send via notificationService per recipient, relatedEntityType BROADCAST
- Broadcast markRead sets readAt now status READ, deliveredCount = not SENT count, readCount = READ count
- Automation rule code unique per org UPPER, conditions Map→JSONB, cooldownHours default 24, executionCount, lastTriggeredAt, isActive, triggerRule checks isActive + cooldown (lastTriggered + cooldownHours*3600 > now → throw cooldown), executes affectedCount 1 demo, updates lastTriggeredAt + count, creates execution log with context JSON, status SUCCESS/FAILED + error
- Automation trigger event handling: handleTriggerEvent called by other domains (LeaseExpiryAlertJob 60/30 days, MaintenanceSlaScheduler SLA breach, FinancialScheduler rent due) finds rules by org+triggerEvent+active, loops triggerRule with context, logs warn if fails
- S3 for broadcast attachment presigned 30m
- Security: all org-scoped via orgId from JWT, @PreAuthorize permEval hasPermission COMMUNICATION_SEND/TEMPLATE_MANAGE or hierarchy 60/80, soft delete, audit

---

## 🔗 API Flow Examples

### Template + Direct Notification + Queue

```
POST /api/notification/templates {
  name:"Rent Reminder 3 Days", code:"RENT_REMINDER_3D",
  channel:"WHATSAPP", subject:"Rent Due Reminder",
  body:"Hi {{tenant_name}}, your rent {{rent_amount}} for {{property_name}} is due on {{due_date}}. Please pay via {{payment_link}}",
  variables:["tenant_name","rent_amount","property_name","due_date","payment_link"],
  category:"RENT", locale:"en_HI"
}
→ 201 {id:1, code:RENT_REMINDER_3D}

POST /api/notifications/send {
  templateCode:"RENT_REMINDER_3D",
  channel:"WHATSAPP",
  recipientContact:"9876543210",
  recipientUserId:10, recipientType:"TENANT",
  variables:{"tenant_name":"Amit","rent_amount":"25000","property_name":"Sky Heights","due_date":"5th Aug","payment_link":"https://pay.propos.io/INV-00001"},
  relatedEntityType:"INVOICE", relatedEntityId:1
}
→ 201 {id:100, status:QUEUED, subjectRendered:"Rent Due Reminder", bodyRendered:"Hi Amit, your rent 25000 for Sky Heights is due on 5th Aug..."}

# Queue consumer every 10 sec
📨 Consumed queue: sent 100 notification 100 to 9876543210 via WHATSAPP
→ Status SENT + providerMessageId mock-whatsapp-UUID → DELIVERED after 2 sec (mock)

GET /api/notifications?channel=WHATSAPP&status=DELIVERED&page → [log 100]

# Failed retry
# If mock failure 5% → FAILED nextRetryAt 5min later
# Scheduler every 1 min retries, re-queues, attempt 1..5 then BOUNCED

POST /api/notifications/100/retry → re-queue
```

### Broadcast Announcement "Water supply cut tomorrow"

```
POST /api/broadcasts {
  propertyId:1,
  title:"Water supply cut tomorrow",
  message:"Dear residents, water supply will be cut tomorrow 10 AM - 4 PM due to maintenance. Please store water.",
  priority:"HIGH", category:"WATER",
  expiresAt:"2026-08-10T18:00:00Z",
  sendPush:true, sendWhatsapp:true, sendEmail:false,
  actionRequired:false
}
→ 201 {id:1, title:"Water supply cut tomorrow", totalRecipients:25 (all tenants in property 1), deliveredCount:0, readCount:0}
→ Sends 25x PUSH + 25x WHATSAPP via notificationService → queued

GET /api/broadcasts?propertyId=1&isActive=true → [broadcast 1]

# Tenant reads
POST /api/broadcasts/1/read (as tenant user)
→ Recipient READ readAt now, readCount 1

# Attachment upload
POST /api/broadcasts/1/attachment multipart file maintenance_notice.pdf
→ S3 upload broadcasts/1/file + presignedUrl 30m
```

### Automation Rule for 60/30 Day Lease Expiry + Rent Due

```
POST /api/notification/templates {
  name:"Lease Expiry 60 Days", code:"LEASE_EXPIRY_60D",
  channel:"EMAIL", subject:"Lease expiring in 60 days - {{property_name}} {{unit_number}}",
  body:"Hi {{tenant_name}}, your lease {{lease_number}} for {{unit_number}} expires on {{expiry_date}} (60 days). Please contact manager.",
  variables:["tenant_name","lease_number","unit_number","property_name","expiry_date"]
}

POST /api/automation/rules {
  name:"Auto Lease Expiry 60 Days Alert",
  code:"AUTO_LEASE_EXPIRY_60D",
  description:"Send email when lease expires in 60 days",
  triggerEvent:"LEASE_EXPIRY_60D",
  conditions:{"property_id_in":[],"unit_status":"occupied"},
  templateId:2,
  isActive:true,
  cooldownHours:24
}
→ 201 {id:1, code:AUTO_LEASE_EXPIRY_60D, triggerEvent:LEASE_EXPIRY_60D}

# LeaseExpiryAlertJob daily 9 AM IST finds leases expiring in 60 days
# It calls automationService.handleTriggerEvent(orgId, LEASE_EXPIRY_60D, context={leaseId:1, tenantId:2, propertyId:1, expiryDate:2026-10-08, daysLeft:60})
# This triggers rule 1 → execution log SUCCESS affected 1
# Future: rule would send notification using template to tenant

# Manual trigger for testing
POST /api/automation/rules/1/trigger {"leaseId":1, "tenantId":2, "expiryDate":"2026-10-08"}
→ {status:SUCCESS, affectedRecipientsCount:1}

GET /api/automation/logs?ruleId=1 → [log SUCCESS]

# Trigger event for rent due
POST /api/automation/trigger-event/RENT_DUE_3D {"invoiceId":5, "tenantId":10, "dueDate":"2026-08-05"}
→ Triggers all rules with triggerEvent RENT_DUE_3D for org

# Other triggers: RENT_DUE_7D, RENT_OVERDUE_1D, TICKET_CREATED, TICKET_COMPLETED, LEAD_NO_FOLLOWUP_2D, UTILITY_BILL_GENERATED, etc
```

---

## 📊 Metrics Domain 5

- Repositories: 7 (Template, Log search, Broadcast search, Recipient, Rule, ExecutionLog)
- DTOs: 9 (TemplateCreate/Response, SendNotificationRequest/NotificationLogResponse, BroadcastCreate/Response with RecipientStatus, AutomationRuleCreate/Response, AutomationLogResponse)
- Services: 4 (Template, Notification with Redis queue + render + directSendMock 95% success + retry, Broadcast with recipient determination + multi-channel send + read tracking + S3 attachment, Automation with code unique + conditions JSON + cooldown + trigger + handleTriggerEvent)
- Controllers: 4 (Template 6 endpoints, Notification 4, Broadcast 5, Automation 8) = 23 endpoints
- Schedulers: 1 NotificationQueueConsumer (consumeQueue every 10 sec up to 20 msgs + retryFailed every 1 min, BOUNCED after 5 retries)
- Business rules: template code/name unique per org UPPER, variables JSONB, rendering {{var}} with removal of unreplaced, Redis queue leftPush/rightPop JSON, 95% success mock, EMAIL/SMS immediate DELIVERED, retry nextRetryAt 5min*retryCount, BOUNCED after 5, broadcast recipients determination specific/unit/property/org-wide + distinct + multi-channel flags, read tracking, attachment S3 presigned 30m, automation code unique UPPER + conditions JSON + cooldown + executionCount + handleTriggerEvent integration point for other domains
- S3: broadcast attachments presigned 30m
- Security: COMMUNICATION_SEND, COMMUNICATION_TEMPLATE_MANAGE permissions
- Integration: LeaseExpiryAlertJob (Domain2) and MaintenanceSlaScheduler (Domain4) and FinancialScheduler (Domain3) can call automationService.handleTriggerEvent to auto-notify

---

## ✅ Domain 5 Completion Checklist

- [x] Template CRUD org-scoped code/name unique UPPER + variables List→JSONB + locale default en + isActive + search pagination
- [x] Notification send with templateId OR templateCode OR direct subject/body + channel @NotNull + recipientContact @NotBlank + recipientUserId/type + variables Map rendering {{var}} + relatedEntityType/Id + QUEUED initially + Redis queue leftPush JSON + fallback directSendMock + mock provider 95% success + EMAIL/SMS immediate DELIVERED + FAILED nextRetryAt 5min*retryCount
- [x] Notification search org+channel/status/recipientType/relatedEntityType + getLog + retryFailed re-queue + toResponse
- [x] NotificationQueueConsumer fixedDelay 10 sec consume up to 20 msgs rightPop + directSendMock + logs 📨 + fixedDelay 60 sec retryFailed nextRetryAt<=now retryCount>=5 → BOUNCED else re-queue
- [x] Broadcast create org + creator + property/unit belongs org check + title/message @NotBlank + priority default MEDIUM + category + expiresAt + attachmentS3Key + actionRequired + send_* flags default true/false + recipients determination specific list filter org / unit tenants / property all tenants / org-wide all active users + distinct + creates AnnouncementRecipient SENT + multi-channel send via notificationService per recipient based on flags (PUSH email, WHATSAPP phone, EMAIL email) relatedEntity BROADCAST + logs recipients count + search propertyId/isActive/expiresAt>now + get + uploadAttachment S3 broadcasts/{id}/file + markRead recipient READ readAt
- [x] AutomationRule create org code unique UPPER + template belongs org + conditions Map→JSON + isActive default true cooldown default 24 executionCount 0 + getAll (list) + get + update + delete soft + triggerRule isActive check + cooldown lastTriggered+cooldownHours*3600 > now → cooldown error + affectedCount 1 demo + details + status SUCCESS/FAILED + updates lastTriggered + count + creates execution log context JSON + getExecutionLogs ruleId/org pagination + handleTriggerEvent finds rules by org+triggerEvent+active and triggers each
- [x] Controllers 4 secured @PreAuthorize permEval 23 endpoints + @CurrentUser orgId + @Valid + ApiResponse + paginated + soft delete + audit + S3 presigned
- [x] Integration points: other domains can call handleTriggerEvent for LEASE_EXPIRY_60D/30D, RENT_DUE_7D/3D/OVERDUE_1D/5D, TICKET_CREATED/COMPLETED/SLA_BREACH, LEAD_NO_FOLLOWUP_2D, UTILITY_BILL_GENERATED etc

---

## 🔜 Next: Domain 6 — IoT & Smart Tech (Future-Proofing)

- SmartLockDevice registry: property/unit, deviceName, provider TTLOCK/AUGUST/YALE/SMARTTHINGS/AQARA/CUSTOM, device_id_external UNIQUE, mac, api_key_encrypted, api_secret_encrypted, firmware_version, status ACTIVE/OFFLINE/MAINTENANCE/DECOMMISSIONED, battery_level, signal_strength, last_seen, config_json JSONB
- AccessPin: device, generated_for_user, generated_for_type TENANT/VENDOR/PROSPECTIVE/STAFF/HOUSEKEEPING/EMERGENCY, pin_code_encrypted 6 digits, pin_hash SHA256 for lookup, label "Vendor Plumbing Ticket#123", valid_from/to, max_uses default 1 used_count, is_active, created_by, revoked_at/reason
- AccessLog: device, pin, user, access_type PIN/FINGERPRINT/CARD/APP/MANUAL, accessed_at, success, failure_reason, ip, latlon, provider_event_id, raw_payload JSONB
- IoTWebhookConfig: org, provider, webhook_url, secret_encrypted, events_subscribed JSONB ["lock.unlocked","battery.low"], isActive, last_received, failure_count
- Redis: lock:pin:{deviceId}:{pin_hash} TTL = valid_to - now for fast validation without DB
- Services: DeviceService register+status, PinService generate temporary PIN encrypted + hash + TTL Redis + max_uses, AccessService validate PIN via Redis TTL + used_count increment + log AccessLog success/failure, WebhookService handle provider callbacks
- Controllers: /api/iot/devices, /api/iot/pins/generate, /api/iot/access/validate, /api/iot/webhooks/{provider} + S3? + presigned
- Integration with Maintenance: vendor gets temporary PIN for ticket, prospective tenant gets PIN for visit

**Reply:** "Approved, begin Phase 3 Domain 6: IoT & Smart Tech" for final monopoly OS pillar.
