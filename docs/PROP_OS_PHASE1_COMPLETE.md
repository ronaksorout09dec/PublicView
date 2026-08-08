# Prop-OS Phase 1 — COMPLETE ✅
## Architecture & Database Design Delivered

**Date:** Aug 08, 2026  
**Branch:** arena/019fe27f-publicview  
**Status:** Ready for Review → Awaiting approval for Phase 2

---

### 📦 Deliverables

#### 1. Design Documentation
- **File:** `docs/PROP_OS_DATABASE_DESIGN.md` (22KB)
  - 6 Pillars explained as DDD Bounded Contexts
  - Multi-tenancy strategy (org_id row-level isolation)
  - 50 Table design with Indexes, Unique Constraints, Performance notes
  - Mermaid ERD with all relations
  - JPA mapping guidelines (LAZY, JSONB, soft-delete, optimistic locking)

#### 2. Production DDL
- **File:** `docs/PROP_OS_DDL.sql` (46KB, 1064 lines)
  - 49 Tables covering all pillars + seed data for 31 Permissions & 7 System Roles
  - PostgreSQL 16 syntax: BIGSERIAL, JSONB, GENERATED columns (balance_due, units_consumed)
  - Extensions: pgcrypto for UUID
  - FK constraints with DEFERRABLE for circular dependencies
  - Partial indexes for performance (vacant units, active locks)

#### 3. JPA Entity Scaffold (82 Java Files)
Location: `backend/src/main/java/com/skyheights/realestate/`

**Foundation:**
- `common/entity/BaseEntity.java` — id, uuid, createdAt (Instant), updatedAt, createdBy, updatedBy, isDeleted, version (optimistic lock)

**Organization & RBAC (7 files):**
- `modules/organization/entity/` — Organization, AppUser, Role, Permission, UserRole
- `modules/organization/enums/` — RoleName (100>80>60>51>50>30>20 hierarchy), OrgStatus, UserStatus, SubscriptionPlan
- Relations: Org 1–N Users, Users N–N Roles via UserRole, Roles N–N Permissions

**Portfolio & CRM (10 files):**
- `modules/portfolio/entity/` — Property, Unit, Amenity (+ M:N join tables via @JoinTable)
- `modules/portfolio/enums/` — PropertyType, PropertyStatus, UnitType, UnitStatus, AmenityCategory
- `modules/crm/entity/` — CrmLead (budget_min/max, ai_score, next_followup), LeadVisit, WaitlistEntry
- `modules/crm/enums/` — LeadStatus, LeadSource, VisitStatus, WaitlistStatus
- Edge: Property->Units, Vacancy index, Unit holds current_tenant_id denormalized for fast scan

**Financial Engine (15 files):**
- `modules/financial/entity/` — Invoice (with balance calc helper), InvoiceLineItem, LateFeeRule, UtilityType, UtilityMeter (ratio_config JSONB), UtilityReading, UtilityBill, UtilityBillSplit (tenant/unit share ratio), SecurityDeposit, SecurityDepositLedger (audit trail), Transaction, TaxReportSnapshot (report_json JSONB)
- `modules/financial/enums/` — InvoiceType/Status, LateFeeType, TransactionType/Category, UtilityTypeEnum, DepositStatus/LedgerType
- Edge Cases Solved:
  - Mid-month proration: billing_period_start/end + helper calculateBalance()
  - Utility split: EQUAL, RATIO, SUBMETER via ratio_config JSONB + UtilityBillSplit share_ratio decimal(5,4)
  - Late fee: grace + % per day + cap + compounding flag
  - Deposit ledger: each deduction linked to ConditionReportItem or Ticket

**Tenant Lifecycle (15 files):**
- `modules/tenant/entity/` — TenantProfile, KycDocument (s3_key front/back, encrypted number), LeaseAgreement (leaseVersion separate from BaseEntity optimistic lock version, rent_due_day 1-28 check), EsignTracking (otp_hash, ip, user_agent), ChecklistTemplate (items JSONB), UnitConditionReport, ConditionReportItem (estimated_repair_cost), ConditionPhoto (metadata JSONB S3)
- `modules/tenant/enums/` — TenantStatus, KycDocumentType, KycStatus, LeaseStatus, EsignStatus, ConditionType, ReportType
- Automation: end_date indexed for Quartz job LeaseExpiry 60/30 days

**Maintenance & Vendor Bidding (12 files):**
- `modules/maintenance/entity/` — VendorProfile (rating, specialization), MaintenanceTicket (sla_due_at, rating_by_tenant), TicketMedia (S3 image/video), VendorBid (unique ticket+vendor), WorkOrder (otp_verified_for_completion), VendorPayout (tds, net_payable, UTR)
- `modules/maintenance/enums/` — TicketStatus (state machine OPEN->BROADCASTED->BIDDING->ASSIGNED->IN_PROGRESS->COMPLETED), TicketPriority, VendorSpecialization, BidStatus, WorkOrderStatus, PayoutStatus

**Communication & Automation (10 files):**
- `modules/communication/entity/` — NotificationTemplate (code UNIQUE per org, variables JSONB, whatsapp_template_id), NotificationLog (provider_message_id, retry_count), BroadcastAnnouncement (property_id nullable = org-wide, send_push/sms/whatsapp/email flags), AnnouncementRecipient (delivered_via JSONB), AutomationRule (trigger_event, cooldown_hours), AutomationExecutionLog (context JSONB)
- `modules/communication/enums/` — NotificationChannel, NotificationStatus, AutomationTrigger (RENT_DUE_7D, LEASE_EXPIRY_60D etc 13 triggers), BroadcastPriority
- Redis: notification queue + automation state

**IoT & Smart Locks (8 files):**
- `modules/iot/entity/` — SmartLockDevice (api_key_encrypted, battery_level), AccessPin (pin_code_encrypted, pin_hash SHA256 for fast lookup, valid_from/to, max_uses/used_count, isActive, revoked_at), AccessLog (raw_payload JSONB provider), IoTWebhookConfig (secret_encrypted, events_subscribed JSONB)
- `modules/iot/enums/` — LockProvider (TTLOCK/AUGUST/YALE...), LockStatus, AccessType, PinType
- Redis: lock:pin:{device_id}:{hash} TTL = valid_to for ultra-fast validation

---

### 🔗 Key Relationships (Excerpt)

```
Organization (Tenant Root)
 ├─ AppUser (staff, accountant, tenant user, vendor user)
 │   └─ UserRole → Role → Permission (31 perms seeded)
 ├─ Property
 │   ├─ Unit
 │   │   ├─ LeaseAgreement → TenantProfile (via AppUser)
 │   │   │   ├─ SecurityDeposit → SecurityDepositLedger
 │   │   │   ├─ EsignTracking
 │   │   │   └─ UnitConditionReport → ConditionReportItem → ConditionPhoto
 │   │   └─ UtilityMeter → UtilityReading
 │   │       └─ UtilityBill → UtilityBillSplit → Invoice
 │   ├─ MaintenanceTicket → TicketMedia → VendorBid → WorkOrder → VendorPayout → Transaction
 │   ├─ SmartLockDevice → AccessPin → AccessLog
 │   └─ BroadcastAnnouncement → AnnouncementRecipient
 ├─ CrmLead → LeadVisit / WaitlistEntry
 ├─ Invoice → InvoiceLineItem / Transaction / TaxReportSnapshot
 └─ NotificationTemplate → NotificationLog / AutomationRule → AutomationExecutionLog
```

---

### ⚙️ Design Decisions & Trade-offs

1. **Modular Monolith over Microservices** for Phase 1 — faster iteration, single DB transaction for invoice creation, can split later via domain events + Outbox table.
2. **Instant vs LocalDateTime** — BaseEntity uses Instant (UTC) for global SaaS, domain dates (billing_month, lease start/end) use LocalDate for calendar correctness.
3. **JSONB for Config** — ratio_config, report_json, variables, templates use JSONB to avoid 10 extra tables, indexed via GIN future.
4. **S3 Never BLOB** — All documents/media store only s3_key (path {org_id}/{entity}/{year}/{uuid}-file), not in PG.
5. **Soft Delete** — is_deleted default false, @Where clause planned for Phase 2 Hibernate Filter.
6. **UUID + BIGINT PK** — BIGINT PK for FK performance, UUID for external API idempotency.
7. **Denormalized current_tenant_id in units** — Allows instant vacancy scan without joining leases, kept in sync via service layer.
8. **Optimistic Lock version vs Business Version** — BaseEntity.version Long for JPA locking, lease_version Int for business lease renewal count.

---

### 📊 Metrics

- Tables: 49 (new) + 1 legacy leads = 50
- Entities: 41 domain + 1 BaseEntity + legacy Lead
- Enums: 41
- Indexes: ~85 (B-Tree + partial + composite)
- Seed Permissions: 31 (PROPERTY_READ/WRITE to ORG_MANAGE)
- System Roles: 7 (SUPER_ADMIN 100 down to VENDOR 20)

---

### 🚀 Next: Phase 2 Plan

**Once approved, I will:**

1. Update `pom.xml`:
   - Spring Security 6, JJWT 0.12.5 (io.jsonwebtoken), Redis starter, AWS SDK S3 v2, Flyway, Validation
   - Spring Boot 3.2.5 Java 21 already present
2. Configure:
   - `application.properties` → PostgreSQL + Redis + S3 + JWT (access 15min, refresh 7d) + Flyway
   - `SecurityConfig` (stateless, BCrypt, CORS), `JwtTokenProvider` (generate/validate), `RedisConfig` (TTL, cache manager), `AuditingConfig` (AuditorAware)
   - `BaseEntity` auditing listener, `S3Service` placeholder, `GlobalExceptionHandler` extended
3. Implement RBAC:
   - `PermissionEvaluator` with hierarchy (MANAGER > STAFF > TENANT), `Role` hierarchy_level check, MethodSecurity `@PreAuthorize("hasPermission(#orgId,'PROPERTY_WRITE')")`
   - Seed seeder `V1__seed_roles_permissions.sql` → auto insert roles/perms on startup via CommandLineRunner
4. Deliverables: Project builds, `/api/auth/login` + `/api/auth/register` + `/api/org` working, JWT flow tested via `TestRestTemplate`, Redis ping OK.

**No business logic yet — pure skeleton + security.**

---

### ✅ Review Checklist for You

- [ ] Does 49-table DDL cover your 6 pillars? Any missing entity e.g Building separate from Property?
- [ ] Is `Organization` as tenant root OK or do you want separate `Portfolio` entity between Org and Property?
- [ ] For Utility Splitting — ratio_config JSONB vs normalized table? I chose JSONB for flexibility (EQUAL/RATIO/SUBMETER)
- [ ] Are `Flat/Bed/Shop` as UnitType enum sufficient or need sub-type config table?
- [ ] Confirm S3 path convention `{org_id}/{entity}/{year}/{uuid}-{filename}` is acceptable

**Reply:** "Approved, begin Phase 2" — I will immediately scaffold Spring Boot project with Security + JWT + Redis + RBAC.

---

*Generated by Elite Enterprise Architect — Prop-OS Phase 1*
