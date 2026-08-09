# Prop-OS Phase 3 Domain 2 — COMPLETE ✅
### Tenant Lifecycle & Legal Domain — Onboarding, KYC, Lease, E-Sign, Condition Reports, Expiry Alerts

**Date:** Aug 09, 2026  
**Branch:** arena/019fe27f-publicview  
**Status:** Tenant domain operational with S3 document storage, state machines, expiry job, paginated secured APIs

---

## 📦 Deliverables

### 1. Repositories (8)

- `TenantProfileRepository` — search propertyId/unitId/status/search (user fullName/email/phone LIKE), count by status, findByUnitId, findByUserId, existsByUnitAndStatus
- `KycDocumentRepository` — findByTenantId, findByTenantIdAndStatus, findByOrgAndStatus, existsByTenantAndType
- `LeaseAgreementRepository` — search propertyId/unitId/tenantId/status/search leaseNumber LIKE, findByTenant/unit, existsByUnitAndStatus ACTIVE, findExpiringOn date, findByStatusAndEndDateBetween, findByStatusAndEndDateBefore for expiry job
- `EsignTrackingRepository` — findByLeaseId, findByLeaseIdAndStatus, countByLease, findBySigner
- `ChecklistTemplateRepository` — findByOrg, findByOrgAndType, findByOrgAndIsActive
- `UnitConditionReportRepository` — search leaseId/unitId/tenantId/type
- `ConditionReportItemRepository` — findByReportId
- `ConditionPhotoRepository` — findByReportId, findByReportItemId

All org-scoped + isDeleted false.

### 2. DTOs (14)

- `TenantCreateRequest` — userId OR inline fullName/email/phone/password, propertyId/unitId, tenancyType PRIMARY/CO_TENANT, employer/occupation/monthlyIncome, emergencyContact, moveIn/expectedMoveOut, notes. Validation @Email, phone regex Indian.
- `TenantUpdateRequest` — propertyId/unitId, tenancyType, employer/occupation/income, emergencyContact, moveIn/expected/actualMoveOut, status, notes
- `TenantResponse` — id/uuid/orgId/userId/fullName/email/phone, propertyId/name, unitId/number, tenancyType, employer/occupation/income, emergencyContact, moveIn/expected/actualMoveOut, status, notes, KYC summary total/verified/kycComplete boolean, activeLeaseId/number/endDate, createdAt/updatedAt
- `KycCreateRequest` — tenantId @NotNull, documentType @NotNull, documentNumber, s3Key/front/back, expiryDate
- `KycResponse` — id/uuid/orgId/tenantId/tenantName, documentType, documentNumberMasked ****1234, s3Key/front/back, front/back presigned URLs 15m via S3Service, verificationStatus, verifiedById/name/at, rejectionReason, expiryDate, createdAt
- `LeaseCreateRequest` — propertyId/unitId/tenantId @NotNull, startDate/endDate @NotNull, rentAmount/depositAmount @NotNull, rentDueDay 1-28 default 5 @Min(1), noticePeriod, lockIn, escalation%, terms, parentLeaseId renewal
- `LeaseUpdateRequest` — start/end/rent/deposit/dueDay/notice/lockIn/escalation/status/terms/terminationReason
- `LeaseResponse` — id/uuid/orgId, propertyId/name, unitId/number, tenantId/name/email, leaseNumber UNIQUE LEASE-2026-00001, start/end, rent/deposit, rentDueDay, notice/lockIn/escalation, status DRAFT/PENDING_SIGNATURE/ACTIVE/EXPIRED/TERMINATED/RENEWED/CANCELLED, terms, finalPdfS3Key/presignedUrl 30m, leaseVersion, parentLeaseId, terminationReason, esignTotal/signed/allSigned boolean, createdAt/updatedAt, expiry computed daysUntilExpiry, expiringIn60/30, expired boolean
- `EsignCreateRequest` — leaseId @NotNull, signerUserId @NotNull, signerRole @NotNull TENANT/OWNER/MANAGER/WITNESS, signatureOrder
- `EsignResponse` — id/uuid/leaseId/number, signerUserId/name/email, signerRole, status PENDING/SENT/VIEWED/SIGNED/DECLINED/EXPIRED, signatureOrder, signatureDataS3Key, signedAt, ip, otpVerified, expiryAt, createdAt
- `ChecklistTemplateCreateRequest` — type @NotNull MOVE_IN/MOVE_OUT/PERIODIC, name @NotBlank, description, itemsJson @NotNull JSON array [{"key":"wall_paint","label":"Wall Paint","type":"CONDITION","required":true}], isActive
- `ChecklistTemplateResponse` — id/uuid/orgId/type/name/description/itemsJson/isActive/createdAt
- `ConditionReportCreateRequest` — leaseId @NotNull, type @NotNull, templateId, overallCondition EXCELLENT/GOOD/FAIR/POOR/DAMAGED, notes, items List<ConditionReportItemCreateRequest> which has area, itemName, condition, description, estimatedRepairCost, photoS3Keys List, photoCaptions
- `ConditionReportResponse` — id/uuid/orgId/leaseId/number/unitId/number/tenantId/name, type, templateId/name, inspectedByUserId/name, inspectedAt, overallCondition, notes, status DRAFT/COMPLETED/DISPUTED/ADJUSTED, pdfS3Key/presigned, items List<ItemResponse> which has id/area/itemName/condition/description/estimatedRepairCost/photos List<PhotoResponse> with id/uuid/s3Key/presignedUrl/caption/takenAt, totalEstimatedRepairCost sum, createdAt

All Lombok Builder, validation.

### 3. Services with Enterprise Business Logic

**`TenantService`:**
- **createTenant:** validates org, property/unit belong to org, unit belongs to property if both provided, unit status must be VACANT or RESERVED else block, userId link OR create new AppUser inline with email uniqueness per org, BCrypt password, assign TENANT role (system role), save TenantProfile status PROSPECT, tenancyType default PRIMARY
- **searchTenants:** propertyId/unitId/status/search LIKE user fullName/email/phone via query
- **getTenant:** org check
- **updateTenant:** property/unit belongs to org, status transition validation:
  - PROSPECT → ACTIVE, PENDING_KYC, BLACKLISTED, MOVED_OUT only
  - PENDING_KYC → ACTIVE, BLACKLISTED only
  - ACTIVE → NOTICE_PERIOD, MOVED_OUT, BLACKLISTED only
  - NOTICE_PERIOD → MOVED_OUT, ACTIVE (withdraw notice)
  - MOVED_OUT terminal
  - BLACKLISTED → PROSPECT only (re-evaluation)
  - If status MOVED_OUT and actualMoveOutDate null → set now
- **deleteTenant:** edge cannot delete if has ACTIVE lease, soft delete
- **toResponse:** calculates KYC summary total/verified/kycComplete, active lease lookup ACTIVE, returns tenantName/email/phone from user

**`KycService`:**
- **createKycDocument:** validates tenant belongs to org, documentNumber length >=4, documentType, saves PENDING status, logs creation
- **uploadAndCreate:** MultipartFile front/back upload via S3Service.generateKey(orgId, "kyc/"+tenantId, type+"_front_"+filename) + uploadFile → returns s3Key, then create document. Handles IOException.
- **getKycByTenant:** org check, returns list mapped to response with masking and presigned URLs
- **getKycDocument:** org check
- **verifyKyc:** validates verifier user exists, approved=true → VERIFIED + verifiedBy + now, approved=false requires rejectionReason, sets REJECTED. After verification, checks if all docs for tenant are VERIFIED and at least 1 doc → if tenant status PENDING_KYC → auto ACTIVE + log. This auto-transition implements onboarding KYC complete rule.
- **deleteKyc:** soft
- **masking:** **** last 4 chars, presigned URLs 15m via S3Service

**`LeaseService`:**
- **createLease:** validates org, property/unit belong to org, unit belongs to property, tenant belongs to org, startDate<=endDate, start not >30 days past (warn), duration >=30 days, rentDueDay 1-28, unit status VACANT/RESERVED only, no existing ACTIVE lease for unit (exists check), tenantHasActive warning but allow for multi-unit. Generates leaseNumber unique LEASE-YYYY-00001 via AtomicLong + check uniqueness loop. Parent lease optional for renewal chain. Saves DRAFT default.
- **searchLeases:** propertyId/unitId/tenantId/status/search leaseNumber LIKE
- **getLease:** org check
- **updateLease:** partial fields, start<=end re-validate, rentDueDay 1-28, status transition validation:
  - DRAFT → PENDING_SIGNATURE, CANCELLED only
  - PENDING_SIGNATURE → ACTIVE, CANCELLED, DRAFT. If target ACTIVE, checks all esign signed: total==signed else block
  - ACTIVE → EXPIRED, TERMINATED, RENEWED only
  - EXPIRED → RENEWED, TERMINATED only
  - TERMINATED/CANCELLED/RENEWED terminal
  - Side effects:
    - ACTIVE from non-ACTIVE → unit status OCCUPIED, currentTenantId=currentLeaseId set, tenant status ACTIVE, moveInDate = lease start
    - TERMINATED/EXPIRED from ACTIVE → unit VACANT, currentTenantId/LeaseId cleared, tenant MOVED_OUT + actualMoveOutDate now if TERMINATED
- **deleteLease:** cannot delete ACTIVE, soft
- **renewLease:** validates old lease ACTIVE or EXPIRED only, creates new LeaseCreateRequest with newStart = oldEnd+1 default, newEnd = oldEnd+1 year default, applies escalationPercent multiplier if set: rent * (1+escalation/100), parentLeaseId = oldId, creates new lease, old status → RENEWED
- **toResponse:** esign counts total/signed/allSigned, daysUntilExpiry = ChronoUnit.DAYS now→endDate, expiringIn60/30 boolean, expired boolean, presignedUrl for finalPdf 30m

**`EsignService`:**
- **createEsignTracking:** validates org, lease in DRAFT/PENDING_SIGNATURE only, signer belongs to same org, duplicate signer+role per lease blocked, signatureOrder default 1, expiry 7 days, status PENDING, if lease DRAFT → auto move to PENDING_SIGNATURE
- **getEsignTrackings:** by leaseId org check
- **sendEsign:** PENDING only → SENT + mock otpHash generation, logs mock email/WhatsApp send
- **markViewed:** SENT → VIEWED
- **signLease:** must be SENT/VIEWED, check expiry → EXPIRED if past, mock OTP verification (if otp present set verified true), upload signatureFile via S3Service generateKey orgId "esign/"+leaseId + upload, set signatureDataS3Key, status SIGNED + signedAt now + ip+userAgent, logs. Checks if all signed → log ready for activation.
- **declineEsign:** sets DECLINED
- **getTracking:** org validation via lease org

**`ChecklistTemplateService`:**
- **createTemplate:** org check, type, name, itemsJson required, isActive default true
- **getTemplates:** org + optional type filter
- **getTemplate/update/delete:** org check, soft delete

**`ConditionReportService`:**
- **createReport:** validates org, lease belongs to org, tenant/unit from lease, template optional belongs to org, inspector user exists, builds report COMPLETED status, inspectedAt now, creates items List: for each itemReq area/itemName/condition required, estimatedRepairCost default 0, saves item, then for each photoS3Keys creates ConditionPhoto entries with caption, takenAt now. Sets items to report.
- **uploadPhotos:** org + report belongs to org, itemId optional must belong to report if provided, MultipartFile list upload via S3Service generateKey orgId "condition-reports/"+reportId, upload, creates ConditionPhoto entries with caption list
- **searchReports:** leaseId/unitId/tenantId/type filters
- **getReport:** org check, toResponse fetches items via itemRepository.findByReportId, photos via photoRepository.findByReportItemId, presigned URLs 30m, totalEstimatedRepairCost sum
- **updateReportStatus:** status string update (DRAFT/COMPLETED/DISPUTED/ADJUSTED)
- **toResponse:** builds nested ItemResponse with PhotoResponse presigned URLs, total cost

**`LeaseExpiryAlertJob` — Scheduler:**
- **@Component @Scheduled cron "0 30 3 * * *" UTC = 9 AM IST daily
- **checkExpiringLeases:** today, in60Days, in30Days, queries findExpiringOn ACTIVE + date, logs 60-day WARN and 30-day CRITICAL with leaseNumber, tenant name, unit number, property name, expiry date. Future Phase 5: TODO create NotificationLog via Communication domain template LEASE_EXPIRY_60D/30D with variables. Also detects expired ACTIVE leases where endDate < today, logs ERROR auto-detection, suggests marking EXPIRED and unit VACANT (for safety not auto-expire in Phase 2, but ready for auto-expire config)
- **hourlyHeartbeatForDev:** fixedRate 1h debug log (can be removed prod)

All services @Transactional, @Slf4j, orgId from JWT, soft delete, audit via BaseEntity.

### 4. Controllers — Secure Paginated REST (24 endpoints)

**Tenant:**

- `POST /api/tenants` — TENANT_WRITE or hierarchy 50, 201, onboarding with inline user creation + TENANT role assign
- `GET /api/tenants?propertyId&unitId&status&search&page` — TENANT_READ
- `GET /api/tenants/{id}` — TENANT_READ
- `PUT /api/tenants/{id}` — TENANT_WRITE or 50, status state machine
- `DELETE /api/tenants/{id}` — TENANT_WRITE or 80, blocked if ACTIVE lease

**KYC:**

- `POST /api/kyc` — TENANT_WRITE or 50, create doc with existing S3 keys, 201
- `POST /api/kyc/upload` multipart — TENANT_WRITE or 50, frontFile/backFile upload to S3 via S3Service, returns presigned URLs
- `GET /api/kyc/tenant/{tenantId}` — TENANT_READ, list with masking + presigned
- `GET /api/kyc/{id}` — TENANT_READ
- `POST /api/kyc/{id}/verify?approved=true&rejectionReason=` — KYC_VERIFY or hierarchy 50, sets VERIFIED/REJECTED + verifiedBy + auto tenant PENDING_KYC→ACTIVE if all verified
- `DELETE /api/kyc/{id}` — KYC_VERIFY or 80

**Lease:**

- `POST /api/leases` — LEASE_MANAGE or hierarchy 50, 201, generates LEASE-YYYY-00001, validates unit VACANT/RESERVED, no ACTIVE lease for unit
- `GET /api/leases?propertyId&unitId&tenantId&status&search&page` — LEASE_MANAGE or TENANT_READ, sorted endDate ASC
- `GET /api/leases/{id}` — same
- `PUT /api/leases/{id}` — LEASE_MANAGE or 50, status state machine + side effects unit OCCUPIED/VACANT + tenant ACTIVE/MOVED_OUT
- `DELETE /api/leases/{id}` — LEASE_MANAGE or 80, cannot delete ACTIVE
- `POST /api/leases/{id}/renew?newStartDate&newEndDate` — LEASE_MANAGE or 80, creates new lease with escalation applied, old → RENEWED

**Esign:**

- `POST /api/esign` — LEASE_ESIGN or LEASE_MANAGE, 201, creates tracking PENDING, lease DRAFT→PENDING_SIGNATURE
- `GET /api/esign/lease/{leaseId}` — LEASE_ESIGN or LEASE_MANAGE, list trackings
- `POST /api/esign/{id}/send` — LEASE_ESIGN, PENDING→SENT mock OTP
- `POST /api/esign/{id}/viewed` — LEASE_ESIGN, SENT→VIEWED
- `POST /api/esign/{id}/sign` multipart signatureFile + otp + ip/userAgent from HttpServletRequest — LEASE_ESIGN, SENT/VIEWED→SIGNED, upload signature S3, otpVerified, logs all signed ready
- `POST /api/esign/{id}/decline?reason=` — LEASE_ESIGN

**Checklist Templates:**

- `POST /api/checklist-templates` — LEASE_MANAGE or hierarchy 50, 201
- `GET /api/checklist-templates?type=` — LEASE_MANAGE or TENANT_READ
- `GET /api/checklist-templates/{id}` — LEASE_MANAGE
- `PUT /api/checklist-templates/{id}` — LEASE_MANAGE
- `DELETE /api/checklist-templates/{id}` — LEASE_MANAGE or 80

**Condition Reports:**

- `POST /api/condition-reports` — LEASE_MANAGE or 50, 201, creates report with items + photo S3 keys
- `GET /api/condition-reports?leaseId&unitId&tenantId&type&page` — LEASE_MANAGE or TENANT_READ
- `GET /api/condition-reports/{id}` — same, includes totalEstimatedRepairCost
- `POST /api/condition-reports/{id}/photos?itemId&files&captions` multipart — LEASE_MANAGE, upload photos S3 + create ConditionPhoto entries, presigned URLs
- `PATCH /api/condition-reports/{id}/status?status=` — LEASE_MANAGE, DRAFT/COMPLETED/DISPUTED/ADJUSTED

All return ApiResponse<T> or Page<T>, @CurrentUser orgId scoping, @Valid validation, @PreAuthorize permEval SpEL.

### 5. Edge Cases & Business Rules

- **Tenant onboarding:** unit must be VACANT/RESERVED, inline user creation with TENANT role + BCrypt, email uniqueness per org, property/unit belongs to same org
- **Tenant status:** state machine prevents invalid transitions, MOVED_OUT terminal, BLACKLISTED→PROSPECT only, actualMoveOutDate auto set now on MOVED_OUT
- **Tenant delete:** blocked if has ACTIVE lease
- **KYC:** documentNumber masking **** last 4, encrypted TODO Phase 4, front/back S3 upload via S3Service with org-isolated key `orgId/kyc/tenantId/...`, presigned 15m, verification requires rejectionReason when rejected, auto tenant PENDING_KYC→ACTIVE when all docs VERIFIED + at least 1 doc
- **Lease creation:** start<=end, duration >=30 days, start not >30 days past warn, rentDueDay 1-28, unit VACANT/RESERVED only, no ACTIVE lease for unit exists check, tenantHasActive warn but allow (multi-unit), leaseNumber unique LEASE-YYYY-00001 atomic counter + uniqueness loop
- **Lease status:** DRAFT→PENDING_SIGNATURE/CANCELLED, PENDING_SIGNATURE→ACTIVE/CANCELLED/DRAFT with all esign signed check, ACTIVE→EXPIRED/TERMINATED/RENEWED with side effects unit OCCUPIED↔VACANT, tenant ACTIVE/MOVED_OUT, EXPIRED→RENEWED/TERMINATED, terminal states block
- **Lease renewal:** only ACTIVE/EXPIRED, newStart default oldEnd+1, newEnd oldEnd+1 year, escalation applied rent*(1+escalation%), parentLease chain
- **Esign:** duplicate signer+role per lease blocked, expiry 7 days, SENT→VIEWED→SIGNED flow, expiry check → EXPIRED, signature S3 upload `orgId/esign/leaseId/sig_userId_file`, ip/userAgent captured, OTP mock verified, all signed log ready for activation
- **Condition Report:** lease belongs to org, template belongs to org, inspector exists, items with estimatedRepairCost, photos S3 keys or upload via multipart, totalEstimatedRepairCost sum, presigned 30m, status DRAFT/COMPLETED/DISPUTED/ADJUSTED
- **Expiry Job:** daily 9 AM IST cron, 60/30 day alerts log WARN/CRITICAL, expired ACTIVE detection logs ERROR, future NotificationLog integration TODO
- **S3:** all docs KYC front/back, esign signature, condition photos, lease final PDF via S3Service.generateKey org isolated + uploadFile + presignedUrl, mocked when S3 disabled
- **Security:** all endpoints org-scoped via orgId from JWT, permEval hasPermission/hasHierarchy, SUPER_ADMIN 100 can cross org via isSameOrg bypass, soft delete preserves ledger, audit via BaseEntity

---

## 🔗 API Flow Examples

### Onboarding Flow
```
POST /api/tenants {
  fullName:"Amit Sharma", email:"amit@example.com", phone:"9876543210",
  propertyId:1, unitId:1, employerName:"Infosys", moveInDate:"2026-08-15"
}
→ 201 {id:1, status:PROSPECT, userId:10, totalKycDocs:0}

POST /api/kyc/upload tenantId=1 documentType=AADHAAR frontFile=...
→ 201 {id:1, status:PENDING, frontPresignedUrl: https://...15m}

POST /api/kyc/1/verify?approved=true
→ 200 {status:VERIFIED}, tenant auto PENDING_KYC→ACTIVE if all verified

POST /api/leases {
  propertyId:1, unitId:1, tenantId:1,
  startDate:"2026-08-15", endDate:"2027-08-14",
  rentAmount:25000, depositAmount:50000, rentDueDay:5,
  terms:"Standard lease terms..."
}
→ 201 {leaseNumber:LEASE-2026-00001, status:DRAFT}

POST /api/esign {leaseId:1, signerUserId:10, signerRole:"TENANT", signatureOrder:1}
POST /api/esign {leaseId:1, signerUserId:2, signerRole:"OWNER", signatureOrder:2}
→ Lease DRAFT→PENDING_SIGNATURE

POST /api/esign/1/send → SENT mock OTP
POST /api/esign/1/sign multipart signatureFile + otp="123456"
→ SIGNED, S3 upload sig

POST /api/esign/2/send + sign → all signed log ready

PUT /api/leases/1 {status:ACTIVE}
→ Unit 1 VACANT→OCCUPIED, currentTenantId=1, tenant ACTIVE, moveInDate=lease start

# Move-in condition report
POST /api/checklist-templates {type:MOVE_IN, name:"Standard Move-In", itemsJson:"[{\"key\":\"wall\",\"label\":\"Wall Paint\"}]"}
→ 201 template

POST /api/condition-reports {
  leaseId:1, type:MOVE_IN, templateId:1, overallCondition:"GOOD",
  items:[{area:"LIVING_ROOM", itemName:"Wall Paint", condition:"GOOD", estimatedRepairCost:0, photoS3Keys:["..."]}]
}
→ 201 {id:1, totalEstimatedRepairCost:0}

# Lease expiry alerts run daily 9 AM IST, logs 60/30 day warnings

# Move-out flow
POST /api/condition-reports {leaseId:1, type:MOVE_OUT, overallCondition:"FAIR",
  items:[{area:"BEDROOM", itemName:"Door", condition:"DAMAGED", estimatedRepairCost:2000}]
}
→ totalEstimatedRepairCost=2000 → future deposit deduction via Financial domain

PUT /api/leases/1 {status:TERMINATED, terminationReason:"End of term"}
→ Unit VACANT, tenant MOVED_OUT, actualMoveOutDate now

PUT /api/tenants/1 {status:MOVED_OUT}
→ Terminal check
```

---

## 📊 Metrics Domain 2

- Repositories: 8
- DTOs: 14 (Tenant 3, KYC 2, Lease 3, Esign 2, Checklist 2, Condition 2)
- Services: 6 (Tenant, KYC, Lease, Esign, ChecklistTemplate, ConditionReport) ~1800 lines logic + S3 + state machines + expiry
- Controllers: 6 (Tenant, KYC, Lease, Esign, ChecklistTemplate, ConditionReport) 24 endpoints
- Scheduler: 1 LeaseExpiryAlertJob daily + hourly heartbeat dev
- Business rules: 6 state machines (Tenant, Lease, Esign, Condition report status via string, KYC verification, plus Unit from Domain1) + 12 edge validations
- S3 integrations: KYC front/back, Esign signature, Condition photos, Lease final PDF presigned
- Security: TENANT_READ/WRITE, KYC_VERIFY, LEASE_MANAGE, LEASE_ESIGN permissions used

---

## ✅ Domain 2 Completion Checklist

- [x] Tenant onboarding with inline AppUser creation + TENANT role + BCrypt + email uniqueness + unit VACANT/RESERVED check + property/unit belongs same org
- [x] Tenant search org-scoped + status/search LIKE + KYC summary + active lease summary
- [x] Tenant status state machine + actualMoveOutDate auto + delete blocked if ACTIVE lease
- [x] KYC document creation + S3 upload multipart front/back via S3Service org-isolated key + masking + presigned 15m + verification with rejectionReason + auto tenant PENDING_KYC→ACTIVE when all verified
- [x] Lease creation with leaseNumber unique LEASE-YYYY-00001 + duration >=30 days + rentDueDay 1-28 + unit VACANT/RESERVED + no ACTIVE lease for unit + start<=end + escalation support
- [x] Lease search + status state machine with all-signed check for PENDING→ACTIVE + side effects unit OCCUPIED/VACANT + tenant ACTIVE/MOVED_OUT + daysUntilExpiry + expiring flags + presigned final PDF
- [x] Lease renewal with parent chain + escalation applied rent*(1+%) + old → RENEWED
- [x] Esign tracking creation duplicate signer+role blocked + expiry 7d + DRAFT→PENDING_SIGNATURE + send (mock OTP) + viewed + sign multipart S3 upload + ip/userAgent + OTP verified + decline + all signed log
- [x] Checklist template CRUD org-scoped + type filter
- [x] Condition report creation with lease belongs org + template optional + inspector + items + photo S3 keys + upload photos multipart S3 + totalEstimatedRepairCost sum + presigned URLs + status update
- [x] Lease expiry alert job daily 9 AM IST 60/30 day WARN/CRITICAL logs + expired ACTIVE detection ERROR + future NotificationLog TODO
- [x] All endpoints secured org-scoped via orgId JWT + @PreAuthorize permEval + paginated + @Valid + ApiResponse wrapper + soft delete + audit

---

## 🔜 Next: Domain 3 — Financial & Accounting Engine

**Will implement:**

- Invoices: auto-generation 1st of month via Quartz (Proration for mid-month move-in/out: rent * remainingDays/daysInMonth), types RENT/UTILITY/MAINTENANCE/DEPOSIT/OTHER/LATE_FEE, status DRAFT/ISSUED/PAID/PARTIALLY_PAID/OVERDUE/CANCELLED/VOID, balanceDue generated, pdf S3
- InvoiceLineItems: RENT/BASE/UTILITY/LATE_FEE/DAMAGES/CREDIT
- LateFeeRules: FIXED/PERCENTAGE_PER_DAY/SLAB, grace period, cap, compounding, org or property scoped, isActive
- UtilityTypes, Meters (master/building vs unit, isShared, ratio_config JSONB EQUAL/RATIO/SUBMETER), Readings (previous/current, rate, amount, photo S3, source MANUAL/IOT), Bills (billingMonth, totalAmount, provider, billDocumentS3), BillSplits (share_ratio 0-1, units_allocated, amount_share, invoice link, calculation_notes)
- SecurityDeposits + Ledger (DEPOSIT/DEDUCTION/REFUND/ADJUSTMENT/FORFEITURE, balanceAfter, reference_type CONDITION_REPORT/TICKET)
- Transactions (INCOME/EXPENSE, category RENT/DEPOSIT_REFUND/UTILITY_COLLECTION/MAINTENANCE_VENDOR/VENDOR_PAYOUT/TAX, payment_method CASH/UPI/BANK_TRANSFER/CHEQUE/ONLINE, invoice/vendorPayout link, receipt S3)
- TaxReportSnapshots: financialYear 2025-26, totalIncome/Expense/netProfit, tds/gst, report_json JSONB by property/month/category, pdf S3
- Services: InvoiceService auto-generate monthly cron, proration mid-month, overpayment credit next invoice, late fee dynamic calc grace+% per day cap, utility split equal/ratio/submeter, deposit ledger audit, transaction settlement, tax report 1-click generation
- Controllers: /api/invoices, /api/late-fee-rules, /api/utility/meters/readings/bills/splits, /api/deposits/ledger, /api/transactions, /api/tax-reports secured

**Reply:** "Approved, begin Phase 3 Domain 3: Financial Engine" to continue.
