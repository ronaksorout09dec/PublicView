# Prop-OS Phase 3 Domain 4 — COMPLETE ✅
### Smart Maintenance & Vendor Bidding — Photo/Video Tickets, Broadcast, Bidding, Work Orders, Payouts with SLA

**Date:** Aug 09, 2026  
**Branch:** arena/019fe27f-publicview  
**Status:** Maintenance domain operational with S3 media, vendor bidding state machine, work order lifecycle, payout tracking with TDS, SLA scheduler

---

## 📦 Deliverables

### 1. Repositories (7)

- `VendorProfileRepository` — search org + specialization + search companyName LIKE + isVerified + pagination, findByOrg+specialization, findByUserId, findByOrgId
- `MaintenanceTicketRepository` — search org + propertyId/unitId/tenantId/status/priority/category/assignedVendorId/search title/description LIKE + pagination, findByTenantId, findByAssignedVendorId, findByStatusAndSlaDueAtBefore (SLA breach), countByStatus/countByAssignedVendor
- `TicketMediaRepository` — findByTicketId, findByTicketIdAndMediaType
- `VendorBidRepository` — findById+org, findByTicketId, findByTicketId pageable, findByVendorId, findByTicketIdAndStatus, existsByTicketAndVendor, countByTicket, findByTicketAndVendor
- `WorkOrderRepository` — findById+org, findByTicketId, findByOrg, findByVendorId, search org+vendorId+status
- `VendorPayoutRepository` — findById+org, findByOrg, findByVendorId, search org+vendorId+status, sumNetPayableByVendorAndStatus (totalPaid/pending)

### 2. DTOs (9)

- **Vendor:** `VendorCreateRequest` userId OR inline fullName/email/phone/password + companyName @NotBlank + specialization @NotNull + yearsExperience/bankAccount/bankIfsc, `VendorResponse` id/uuid/orgId/userId/fullName/email/phone/companyName/specialization/yearsExperience/rating/totalJobsCompleted/isVerified/status/createdAt + totalPaid (sum PAID) + pendingPayout (PENDING+APPROVED)
- **Ticket:** `TicketCreateRequest` propertyId @NotNull, unitId, tenantId, category @NotBlank PLUMBING/ELECTRICAL/etc, priority, title @NotBlank, description @NotBlank, mediaS3Keys/mediaTypes pre-uploaded, `TicketResponse` id/uuid/orgId/propertyId/name/unitId/number/tenantId/name/raisedByUserId/name/category/priority/title/description/status OPEN/BROADCASTED/BIDDING/ASSIGNED/IN_PROGRESS/PENDING_PARTS/COMPLETED/CANCELLED/CLOSED/assignedVendorId/name/assignedBidId/estimatedCost/actualCost/scheduledAt/completedAt/completionNotes/ratingByTenant 1-5/feedback/slaDueAt boolean slaBreached = now> slaDue and not COMPLETED/CLOSED/CANCELLED + media List<MediaResponse> id/uuid/s3Key/presignedUrl 30m/mediaType/fileSize/caption/createdAt + bidsCount + createdAt/updatedAt
- **Bid:** `BidCreateRequest` ticketId @NotNull, bidAmount @NotNull @Positive, estimatedDays @NotNull, proposal, includesMaterial, warrantyDays, `BidResponse` id/uuid/orgId/ticketId/title/vendorId/companyName/name/bidAmount/estimatedDays/proposal/status SUBMITTED/APPROVED/REJECTED/WITHDRAWN/EXPIRED/submittedAt/approvedAt/rejectionReason/includesMaterial/warrantyDays/createdAt
- **WorkOrder:** `WorkOrderResponse` id/uuid/orgId/ticketId/title/vendorId/companyName/bidId/assignedByUserId/name/status CREATED/ACCEPTED/IN_PROGRESS/PENDING_APPROVAL/COMPLETED/CANCELLED/scheduledDate/startDate/completedDate/completionNotes/checklistCompleted/otpVerifiedForCompletion/invoiceS3Key/presignedUrl/createdAt/updatedAt
- **Payout:** `PayoutCreateRequest` workOrderId @NotNull, tdsDeducted, paymentMethod UPI/BANK_TRANSFER/CASH, notes, `PayoutResponse` id/uuid/orgId/workOrderId/ticketId/title/vendorId/companyName/amount/tdsDeducted/netPayable/amount-tds/status PENDING/APPROVED/PAID/FAILED/ON_HOLD/paymentMethod/utrNumber/transactionId/paidAt/paidByUserId/name/notes/invoiceS3Key/presignedUrl/createdAt

### 3. Services with Business Logic

**`VendorService`:**
- **createVendor:** org exists, userId link OR create new AppUser inline email uniqueness per org BCrypt + assign VENDOR role system, vendor profile exists for user check duplicate blocked, companyName/specialization/yearsExperience/bankAccount encrypted TODO, rating 0 totalJobs 0 isVerified false status ACTIVE
- **searchVendors:** org + specialization/search companyName LIKE/isVerified + pagination rating DESC
- **getVendor/verifyVendor:** org check, isVerified set, logs
- **deleteVendor:** soft, isDeleted true
- **toResponse:** totalPaid sumNetPayable PAID, pendingPayout sum PENDING+APPROVED

**`TicketService`:**
- **createTicket:** org, property belongs org, unit belongs property if provided, tenant belongs org if provided, raisedBy user exists, SLA due calculated via `calculateSlaDue(priority)`: URGENT 4h, HIGH 24h, MEDIUM 72h, LOW 168h (7 days) Instant now + hours, builds OPEN status, saves, handles pre-uploaded mediaS3Keys + mediaTypes → TicketMedia entries with uploadedBy
- **searchTickets:** org + propertyId/unitId/tenantId/status/priority/category/assignedVendorId/search title/description LIKE
- **getTicket:** org check
- **updateTicketStatus:** state machine validation:
  - OPEN → BROADCASTED/BIDDING/CANCELLED
  - BROADCASTED → BIDDING/CANCELLED
  - BIDDING → ASSIGNED/CANCELLED
  - ASSIGNED → IN_PROGRESS/CANCELLED
  - IN_PROGRESS → COMPLETED/PENDING_PARTS/CANCELLED
  - PENDING_PARTS → IN_PROGRESS/CANCELLED
  - COMPLETED → CLOSED
  - CANCELLED/CLOSED terminal
  - If COMPLETED/CLOSED set completedAt now
- **broadcastTicket:** only OPEN can be broadcasted, status → BROADCASTED, finds matching vendors by specialization = ticket category via vendorRepository.findByOrgAndSpecialization, logs count, TODO Phase5 NotificationLog for each vendor via Communication, then status → BIDDING to allow bids
- **addMedia:** org + ticket belongs org, uploader user exists, MultipartFile list upload via S3Service.generateKey orgId "maintenance/tickets/"+ticketId + uploadFile, detectMediaType contentType image→IMAGE video→VIDEO else DOCUMENT, caption list, saves media entries, logs
- **rateTicket:** rating 1-5 validation, only COMPLETED/CLOSED tickets can be rated, sets ratingByTenant + feedback, updates vendor rating simple avg: (oldRating*totalJobs + newRating)/(totalJobs+1) setScale 2, totalJobsCompleted increment
- **toResponse:** fetches media list via mediaRepository.findByTicketId + presigned 30m, slaBreached = now > slaDueAt and status not COMPLETED/CLOSED/CANCELLED, bidsCount via bidRepository.countByTicketId

**`BidService`:**
- **submitBid:** ticket belongs org, status BIDDING or BROADCASTED only, vendor profile by vendorUserId (current user) must exist and belong to same org, exists check ticket+vendor duplicate blocked, specialization mismatch warning but allow, builds SUBMITTED status submittedAt now, includesMaterial default false warrantyDays default 0, saves, logs
- **getBidsForTicket/getBid:** org check
- **approveBid:** ticket must be BIDDING/BROADCASTED, bid must be SUBMITTED, sets bid APPROVED + approvedAt now, rejects all other SUBMITTED bids for same ticket with rejectionReason "Another bid approved", assigns vendor to ticket assignedVendor = bid.vendor, assignedBidId = bid.id, estimatedCost = bidAmount, ticket status → ASSIGNED, saves ticket, logs
- **rejectBid:** only SUBMITTED can be rejected, sets REJECTED + reason, saves
- **withdrawBid:** vendor owns bid check vendorUserId → vendor profile, only SUBMITTED can be withdrawn, sets WITHDRAWN
- **toResponse:** vendorCompanyName, vendorName from user.fullName

**`WorkOrderService`:**
- **createWorkOrder:** ticket must be ASSIGNED, work order not exists for ticket already, bid belongs org and belongs to ticket, assignedBy user exists, builds CREATED status scheduledDate optional, checklistCompleted false otpVerified false, saves, ticket status → IN_PROGRESS
- **searchWorkOrders/getWorkOrder/getByTicket:** org check + vendorId/status filters
- **updateStatus:** state machine:
  - CREATED → ACCEPTED/CANCELLED
  - ACCEPTED → IN_PROGRESS/CANCELLED
  - IN_PROGRESS → PENDING_APPROVAL/COMPLETED/CANCELLED
  - PENDING_APPROVAL → COMPLETED/IN_PROGRESS
  - COMPLETED/CANCELLED terminal
  - If IN_PROGRESS and startDate null set now, if COMPLETED set completedDate now + completionNotes + ticket status → COMPLETED + completedAt now + actualCost = bidAmount or estimatedCost + completionNotes + ticketRepository save
- **uploadInvoice:** MultipartFile invoice file upload via S3Service orgId "maintenance/workorders/"+id + upload, set invoiceS3Key, save, logs
- **toResponse:** presigned invoice 30m

**`PayoutService`:**
- **createPayout:** workOrder must be COMPLETED, payout not exists for work order already and status != FAILED, amount = bidAmount or ticket estimatedCost, tds default 0, netPayable = amount - tds, status PENDING, paymentMethod/notes, saves, logs
- **searchPayouts/getPayout:** org + vendorId/status filters, sumNetPayable
- **approvePayout:** only PENDING → APPROVED
- **markPaid:** only APPROVED/PENDING → PAID, paidBy user exists, paidAt now, utrNumber/paymentMethod set, invoiceFile optional upload via S3Service orgId "payouts/"+id + upload set invoiceS3Key, creates Transaction for accounting type EXPENSE category VENDOR_PAYOUT amount netPayable date now description "Vendor payout for ticket X work order Y" paymentMethod + createdByUser paidBy, saves transaction, payout transaction = transaction, saves payout, logs UTR + transaction id
- **failPayout:** sets FAILED + notes append FAILED: reason
- **toResponse:** presigned invoice 30m

### 4. Schedulers

**`MaintenanceSlaScheduler`:**
- **checkSlaBreaches:** @Scheduled fixedRate 30 min (1800000 ms), finds tickets status IN_PROGRESS, ASSIGNED, OPEN, BIDDING where slaDueAt < now and isDeleted false, logs WARN SLA BREACH with ticket id/title/category/priority/status/SLA due/now/property/org, TODO Phase5 Notification to manager + tenant via Communication
- **dailySlaSummary:** @Scheduled cron 0 0 8 * * * daily 8 AM UTC = 1:30 PM IST, placeholder for org-wise SLA summary (logs)

### 5. Controllers — Secure REST (23 endpoints)

- **Vendor:**
  - `POST /api/vendors` — VENDOR_MANAGE or hierarchy 50, 201, inline user creation + VENDOR role
  - `GET /api/vendors?specialization&search&isVerified&page` — VENDOR_MANAGE or TICKET_MANAGE, pagination rating DESC
  - `GET /api/vendors/{id}` — same
  - `POST /api/vendors/{id}/verify?verified=true` — VENDOR_MANAGE or 80
  - `DELETE /api/vendors/{id}` — VENDOR_MANAGE or 80

- **Ticket (Maintenance):**
  - `POST /api/maintenance/tickets` — TICKET_CREATE or TICKET_MANAGE, 201, photo/video S3 support via pre-uploaded keys or later /media endpoint, SLA auto calculated
  - `GET /api/maintenance/tickets?propertyId&unitId&tenantId&status&priority&category&assignedVendorId&search&page` — TICKET_MANAGE or TICKET_CREATE, paginated createdAt DESC
  - `GET /api/maintenance/tickets/{id}` — same
  - `PATCH /api/maintenance/tickets/{id}/status?status=` — TICKET_MANAGE, state machine
  - `POST /api/maintenance/tickets/{id}/broadcast` — TICKET_MANAGE, OPEN→BROADCASTED→BIDDING, finds matching vendors by specialization, logs count, future notification
  - `POST /api/maintenance/tickets/{id}/media` multipart files+mediaTypes+captions — TICKET_CREATE or TICKET_MANAGE, S3 upload `orgId/maintenance/tickets/{id}/file`, TicketMedia entries, presigned 30m
  - `POST /api/maintenance/tickets/{id}/rate?rating&feedback` — TICKET_CREATE or TICKET_MANAGE, 1-5 rating, updates vendor rating avg

- **Bid (Vendor Bidding):**
  - `POST /api/vendor/bids` — VENDOR_BID or TICKET_MANAGE, 201, submit bid amount/estimatedDays/proposal, vendorUserId = currentUser if VENDOR role else currentUser, ticket must be BIDDING/BROADCASTED, duplicate ticket+vendor blocked
  - `GET /api/vendor/bids/ticket/{ticketId}?page` — TICKET_MANAGE or VENDOR_BID, pagination bidAmount ASC (cheapest first)
  - `GET /api/vendor/bids/{id}` — same
  - `POST /api/vendor/bids/{id}/approve` — TICKET_MANAGE or hierarchy 50, approves bid, rejects others, assigns vendor to ticket ASSIGNED, estimatedCost = bidAmount
  - `POST /api/vendor/bids/{id}/reject?reason=` — TICKET_MANAGE
  - `POST /api/vendor/bids/{id}/withdraw` — VENDOR_BID, vendor owns bid check, only SUBMITTED can be withdrawn

- **WorkOrder:**
  - `POST /api/work-orders?ticketId&bidId&scheduledDate` — TICKET_MANAGE, 201, ticket must be ASSIGNED, work order not exists for ticket, bid belongs ticket, scheduledDate optional, ticket → IN_PROGRESS
  - `GET /api/work-orders?vendorId&status&page` — TICKET_MANAGE or VENDOR_BID
  - `GET /api/work-orders/{id}` — same
  - `GET /api/work-orders/ticket/{ticketId}` — same
  - `PATCH /api/work-orders/{id}/status?status&completionNotes` — TICKET_MANAGE or VENDOR_BID, state machine CREATED→ACCEPTED→IN_PROGRESS→PENDING_APPROVAL/COMPLETED, COMPLETED sets ticket COMPLETED + completedAt + actualCost + completionNotes
  - `POST /api/work-orders/{id}/invoice` multipart file — TICKET_MANAGE or VENDOR_BID, S3 upload `orgId/maintenance/workorders/{id}/file`, invoiceS3Key

- **Payout (Vendor Payout Tracking):**
  - `POST /api/vendor/payouts` body workOrderId, tdsDeducted, paymentMethod, notes — VENDOR_PAYOUT_MANAGE or hierarchy 60, 201, workOrder must be COMPLETED, payout not exists for work order, amount = bidAmount or estimatedCost, net = amount - tds, PENDING
  - `GET /api/vendor/payouts?vendorId&status&page` — VENDOR_PAYOUT_MANAGE or VENDOR_BID
  - `GET /api/vendor/payouts/{id}` — same
  - `POST /api/vendor/payouts/{id}/approve` — VENDOR_PAYOUT_MANAGE or 80, PENDING→APPROVED
  - `POST /api/vendor/payouts/{id}/pay` multipart invoiceFile + utrNumber + paymentMethod — VENDOR_PAYOUT_MANAGE or 80, APPROVED/PENDING→PAID, paidBy currentUser, paidAt now, upload invoice S3 `orgId/payouts/{id}/file`, creates Transaction EXPENSE VENDOR_PAYOUT netPayable + UTR, marks PAID
  - `POST /api/vendor/payouts/{id}/fail?reason=` — VENDOR_PAYOUT_MANAGE

All @CurrentUser orgId scoping, @PreAuthorize permEval hasPermission/hasHierarchy, @Valid, ApiResponse wrapper, Pageable, soft delete, audit.

---

## 🔗 API Flow Example — Full Bidding Lifecycle

```
# Tenant raises ticket with photo
POST /api/maintenance/tickets {
  propertyId:1, unitId:1, tenantId:1,
  category:"PLUMBING", priority:"HIGH",
  title:"Bathroom leakage", description:"Water leaking from ceiling"
}
→ 201 {id:1, status:OPEN, slaDueAt: 24h from now (HIGH 24h)}

POST /api/maintenance/tickets/1/media multipart 2 images
→ {media: [{s3Key:orgId/maintenance/tickets/1/photo1.jpg, presignedUrl 30m}]}

# Manager broadcasts to matching vendors
POST /api/maintenance/tickets/1/broadcast
→ Status OPEN→BROADCASTED→BIDDING, logs "Broadcasted to 3 vendors of specialization PLUMBING"

# Vendors submit bids (as VENDOR role users)
POST /api/vendor/bids {ticketId:1, bidAmount:1500, estimatedDays:2, proposal:"Will fix with warranty", warrantyDays:30}
→ Vendor A bid 1500

POST /api/vendor/bids {ticketId:1, bidAmount:1200, estimatedDays:3, proposal:"Cheaper but 3 days"}
→ Vendor B bid 1200

GET /api/vendor/bids/ticket/1?page → sorted bidAmount ASC: B 1200, A 1500

# Manager approves best bid
POST /api/vendor/bids/2/approve
→ Bid 2 APPROVED, other bids REJECTED, ticket ASSIGNED, assignedVendor B, estimatedCost 1200

# Create work order
POST /api/work-orders?ticketId=1&bidId=2&scheduledDate=2026-08-10
→ WorkOrder CREATED, ticket IN_PROGRESS

PATCH /api/work-orders/1/status?status=IN_PROGRESS
→ IN_PROGRESS startDate now

PATCH /api/work-orders/1/status?status=COMPLETED&completionNotes=Fixed leakage, replaced pipe
→ COMPLETED completedDate now, ticket COMPLETED completedAt now actualCost 1200

# Rate ticket
POST /api/maintenance/tickets/1/rate?rating=5&feedback=Excellent work
→ Ticket rating 5, vendor rating updated avg (old 0*0 +5)/1=5.0 totalJobs 1

# Payout flow
POST /api/vendor/payouts {workOrderId:1, tdsDeducted:120, paymentMethod:BANK_TRANSFER}
→ Payout amount 1200 tds 120 net 1080 status PENDING

POST /api/vendor/payouts/1/approve → APPROVED

POST /api/vendor/payouts/1/pay multipart invoiceFile + utrNumber=UTR123456 + paymentMethod=BANK_TRANSFER
→ PAID paidAt now paidBy manager, UTR set, invoice S3 upload, Transaction created EXPENSE VENDOR_PAYOUT 1080

GET /api/vendor/payouts?vendorId=1 → totalPaid 1080, pending 0

# SLA breach check every 30 min
# If ticket OPEN and slaDueAt passed, logs WARN SLA BREACH
# Daily summary placeholder
```

---

## 📊 Metrics Domain 4

- Repositories: 7 (Vendor, Ticket, Media, Bid, WorkOrder, Payout + counts/sums)
- DTOs: 9 (VendorCreate/Response, TicketCreate/Response with MediaResponse, BidCreate/Response, WorkOrderResponse, PayoutCreate/Response)
- Services: 5 (Vendor, Ticket with SLA calc/state machine/media S3/rating, Bid with approve/reject/withdraw + ticket assign + other bids reject, WorkOrder with status side effects ticket COMPLETED, Payout with TDS net + transaction creation + S3 invoice)
- Controllers: 5 (Vendor, Ticket 7 endpoints, Bid 6, WorkOrder 6, Payout 6) = 25 endpoints
- Schedulers: 1 MaintenanceSlaScheduler (30 min SLA breach check + daily summary)
- Business rules: 4 state machines (Ticket 7 states, Bid 5, WorkOrder 6, Payout 5) + 12 edge validations (unit belongs property, tenant belongs org, vendor belongs org, duplicate bid, specialization mismatch warn, work order exists, ticket not BIDDING, payout exists, TDS net calc, rating 1-5, mediaType detection, SLA hours per priority)
- S3: ticket photo/video/document media, work order invoice, payout invoice all org-isolated + presigned 30m
- Security: TICKET_CREATE/MANAGE, VENDOR_MANAGE/BID, VENDOR_PAYOUT_MANAGE permissions

---

## ✅ Domain 4 Completion Checklist

- [x] Vendor onboarding inline user creation + VENDOR role + companyName/specialization + rating 0 + isVerified false + totalPaid/pendingPayout sums + soft delete
- [x] Vendor search org + specialization/search companyName LIKE/isVerified + pagination rating DESC + verify
- [x] Ticket raised by tenant with property/unit/tenant belongs org + category/priority/title/description + SLA due 4h/24h/72h/168h based on priority + OPEN status + pre-uploaded S3 keys + media entries
- [x] Ticket search org + property/unit/tenant/status/priority/category/assignedVendor/search + pagination + media presigned + slaBreached boolean + bidsCount + SLA breach scheduler every 30 min logs WARN + daily summary placeholder
- [x] Ticket status state machine OPEN→BROADCASTED/BIDDING/CANCELLED etc + completedAt + broadcast finds matching vendors by specialization + BIDDING to allow bids + media upload multipart S3 orgId/maintenance/tickets/{id}/file + rate 1-5 updates vendor rating avg
- [x] Bid submit vendor profile by userId + ticket BIDDING/BROADCASTED only + duplicate ticket+vendor blocked + specialization mismatch warn + SUBMITTED + search ticketId pagination bidAmount ASC cheapest first + approve (only SUBMITTED, ticket BIDDING, approves bid APPROVED + rejects others SUBMITTED→REJECTED + assigns vendor to ticket ASSIGNED estimatedCost bidAmount) + reject + withdraw vendor owns check
- [x] WorkOrder create ticket ASSIGNED only + work order not exists for ticket + bid belongs ticket + assignedBy + CREATED + ticket→IN_PROGRESS + search vendorId/status + getByTicket + status state machine CREATED→ACCEPTED→IN_PROGRESS→PENDING_APPROVAL/COMPLETED + COMPLETED sets ticket COMPLETED + completedAt + actualCost + completionNotes
- [x] WorkOrder invoice upload S3 orgId/maintenance/workorders/{id}/file
- [x] Payout create workOrder COMPLETED only + payout not exists + amount bidAmount or estimatedCost + TDS net = amount-tds + PENDING + search vendorId/status + approve PENDING→APPROVED + markPaid APPROVED/PENDING→PAID + paidBy + paidAt + UTR + paymentMethod + invoice S3 upload + creates Transaction EXPENSE VENDOR_PAYOUT netPayable + transaction link + fail
- [x] All endpoints secured org-scoped @PreAuthorize permEval + paginated + @Valid + ApiResponse + soft delete + audit + S3 presigned

---

## 🔜 Next: Domain 5 — Communication & Automation

- NotificationTemplate with code UNIQUE per org, variables JSONB, whatsapp_template_id, channel EMAIL/SMS/WHATSAPP/PUSH/IN_APP, subject/body with {{variables}}, category RENT/LEASE/MAINTENANCE/ANNOUNCEMENT/GENERAL, locale en/hi/en_HI_mix, isActive
- NotificationLog with templateId, channel, recipient_type, recipient_id, recipient_contact, subject/body rendered, status QUEUED/SENT/FAILED/DELIVERED/READ/BOUNCED, provider_message_id, failure_reason, related_entity_type/id, retry_count/next_retry
- BroadcastAnnouncement with propertyId null=org-wide, unitId, title/message/priority LOW/MEDIUM/HIGH/CRITICAL, category WATER/ELECTRICITY/MAINTENANCE/EVENT/SAFETY/GENERAL, createdBy, expiresAt, attachment S3, action_required, send_push/sms/whatsapp/email flags + recipients tracking read/delivered
- AutomationRule with code UNIQUE, trigger_event RENT_DUE_7D/3D/OVERDUE_1D/5D/LEASE_EXPIRY_60D/30D/EXPIRED/TICKET_CREATED/COMPLETED/LEAD_NO_FOLLOWUP_2D/UTILITY_BILL_GENERATED etc, conditions JSONB, templateId, isActive, cooldown_hours, last_triggered + ExecutionLog with context JSONB
- Redis queue `notification:queue` + scheduled poller every 10s mock send via providers + retry logic
- Controllers: /api/notification/templates, /api/notification/logs, /api/broadcasts, /api/automation/rules + trigger + logs
- Integration with Domain 2 LeaseExpiryAlertJob and Domain 4 SlaScheduler to auto-create notifications

**Reply:** "Approved, begin Phase 3 Domain 5: Communication & Automation" to continue.
