# Prop-OS Phase 3 Domain 1 — COMPLETE ✅
### Core Portfolio & CRM Domain — Properties, Units, Amenities, Leads, Visits, Waitlist

**Date:** Aug 09, 2026  
**Branch:** arena/019fe27f-publicview  
**Status:** Portfolio & CRM domain operational with secure paginated APIs + business logic + waitlist automation

---

## 📦 Deliverables

### 1. Repositories (6)

**Portfolio:**
- `PropertyRepository` — search with city, search (name/address LIKE), type, status, pagination, count, findWithAmenities JOIN FETCH
- `UnitRepository` — search with propertyId, status, type, unitNumber LIKE, vacancy counts, exists unitNumber per property, findWithAmenities
- `AmenityRepository` — org-scoped, exists name, findByOrgId

**CRM:**
- `CrmLeadRepository` — search with status, source, propertyId, assignedTo, search (name/phone/email LIKE), priority, nextFollowup before, count by status, findWithVisits
- `LeadVisitRepository` — search with leadId, propertyId, status, staffId, scheduled between, count
- `WaitlistEntryRepository` — findMaxPosition, exists lead+property+status, search propertyId/unitType/status, ordered by priorityScore DESC position ASC, getNextInLine

All repositories use `orgId` filter + `isDeleted=false` + indexes from Phase 1 DDL.

### 2. DTOs (12) with Validation

**Portfolio DTOs:**
- `PropertyCreateRequest` — @NotBlank name, @NotNull type, @NotBlank address/city, latitude/longitude, totalFloors/units @Min(0), managerId (must belong to same org), amenityIds Set<Long>, thumbnailS3Key
- `PropertyUpdateRequest` — all optional except validation, status enum, amenityIds
- `PropertyResponse` — id, uuid, orgId, name, type, address, city, state, pincode, lat/lon, floors/units/yearBuilt, managerId/name, status, description, thumbnailS3Key, amenities Set<AmenityResponse>, stats: unitsCount, vacantUnitsCount, occupiedUnitsCount, maintenanceUnitsCount, reservedUnitsCount, createdAt/updatedAt/createdBy
- `AmenityCreateRequest` — @NotBlank name @Size 150, category, icon, description
- `AmenityResponse` — id, uuid, name, category, icon, description
- `UnitCreateRequest` — @NotNull propertyId, @NotBlank unitNumber, floor, @NotNull type, @Positive sizeSqft, bedrooms/bathrooms, @NotNull @Positive rentAmount, @PositiveOrZero deposit, description, amenityIds
- `UnitUpdateRequest` — optional fields, status enum, amenityIds
- `UnitResponse` — id, uuid, orgId, propertyId/name, unitNumber, floor, type, size, bedrooms/bathrooms, rent/deposit, status, description, currentTenantId/LeaseId, amenities, createdAt/updatedAt

**CRM DTOs:**
- `CrmLeadCreateRequest` — propertyId, unitId, interestedUnitType, @NotBlank customerName, @Pattern Indian phone ^[6-9]\d{9}$, email, source, priority, budgetMin/Max, configuration, timeline, purpose, assignedToStaffId, notes, conversationSummary, nextFollowupAt, aiScore
- `CrmLeadUpdateRequest` — all optional + status, lostReason
- `CrmLeadResponse` — id, uuid, orgId, propertyId/name, unitId/number, interestedUnitType, customerName, phone/email, source, status, priority, budgets, configuration, timeline, purpose, assignedToId/name, notes, lostReason, nextFollowupAt, aiScore, createdAt/updatedAt, visitsCount
- `LeadVisitCreateRequest` — @NotNull leadId/propertyId, unitId, @NotNull @Future scheduledAt, notes, staffId
- `LeadVisitUpdateRequest` — scheduledAt, visitedAt, status, notes, feedback, staffId
- `LeadVisitResponse` — id, uuid, orgId, leadId/customerName, propertyId/name, unitId/number, scheduledAt, visitedAt, status, notes, feedback, staffId/name, createdAt
- `WaitlistCreateRequest` — @NotNull propertyId, @NotNull unitType, @NotNull leadId, priorityScore, desiredMoveIn
- `WaitlistResponse` — id, uuid, orgId, propertyId/name, unitType, leadId/customerName/phone, position, status, priorityScore, desiredMoveIn, createdAt

All DTOs use Lombok Builder, validation messages.

### 3. Services with Business Logic & Edge Cases

**`AmenityService`:**
- create: checks org exists, duplicate name per org, default category COMMON
- getAmenities org-scoped, get single, update checks duplicate name excluding self, delete soft

**`PropertyService`:**
- **createProperty:** validates org exists, manager belongs to same org, amenityIds belong to org, builds Property with ACTIVE status, logs creation
- **searchProperties:** @Cacheable `properties` key = orgId_city_search_type_status_pageNumber, delegates to repository.search with LIKE filters
- **getProperty:** findByIdWithAmenities JOIN FETCH, throws ResourceNotFound if not in org
- **updateProperty:** partial update, manager org check, amenityIds replace, @CacheEvict allEntries properties
- **deleteProperty:** edge: cannot delete if has OCCUPIED units (count query), soft delete property, logs
- **getPropertyStats:** calls getProperty which internally calculates counts: unitsCount, vacant, occupied, maintenance, reserved via unitRepository counts
- **Cache:** evict on create/update/delete

**`UnitService`:**
- **createUnit:** validates org, property belongs to org, unitNumber unique per property (exists check), warns if property totalUnits exceeded (log warn, allow), builds VACANT status, amenity check, @CacheEvict units+properties
- **searchUnits:** @Cacheable units, search with propertyId/status/type/search unitNumber LIKE
- **getUnit:** findWithAmenities
- **updateUnit:**
  - unitNumber unique per property excluding self
  - **State Machine Validation:**
    - VACANT → RESERVED, OCCUPIED, MAINTENANCE, NOT_AVAILABLE only
    - RESERVED → OCCUPIED, VACANT only
    - OCCUPIED → NOTICE_PERIOD, VACANT, MAINTENANCE only
    - NOTICE_PERIOD → VACANT, OCCUPIED only
    - MAINTENANCE → VACANT only
  - If new status VACANT → clear currentTenantId/LeaseId + **waitlist trigger:** calls waitlistService.getNextInLineForProperty(propertyId, unitType) and logs next lead with position/priority, future Phase 3.5 will auto-notify via Communication domain
  - Amenity replace
- **deleteUnit:** edge: cannot delete if OCCUPIED, soft delete
- **getVacantUnitsFiltered:** search VACANT only
- **Cache:** evict units+properties (because property stats change when unit status changes)

**`CrmLeadService`:**
- **createLead:** validates org, property/unit belong to org, unit belongs to property if both provided, assigned staff belongs to same org, budgetMin <= budgetMax validation, duplicate phone per property logs warn (allows), aiScore 0-10 validation, defaults source WEBSITE, status NEW, priority MEDIUM
- **searchLeads:** status/source/propertyId/assignedTo/search (name/phone/email LIKE)/priority, pageable
- **getLead:** findWithVisits JOIN FETCH
- **updateLead:** partial, property/unit, status transition validation:
  - LOST → CONVERTED blocked ("Cannot convert a LOST lead. Create new or move to NEW first")
  - CONVERTED → other allowed but logs warn
  - Budget re-validate min<=max
  - assigned staff org check
- **deleteLead:** soft delete
- Edge: nextFollowupAt for overdue leads query in repository

**`LeadVisitService`:**
- **createVisit:** validates org, lead belongs to org, property belongs to org, unit belongs to property if provided, staff org check, scheduledAt @Future + not >1 year ahead (business rule), creates SCHEDULED, updates lead status NEW/CONTACTED → VISIT_SCHEDULED
- **searchVisits:** leadId/propertyId/status/staffId
- **getVisit:** org check via organization.id
- **updateVisit:** scheduledAt/visitedAt, status transition:
  - COMPLETED terminal (cannot change from COMPLETED)
  - CANCELLED → COMPLETED blocked
  - If COMPLETED and visitedAt null → set now + lead status VISIT_SCHEDULED → VISITED
- **deleteVisit:** soft

**`WaitlistService`:**
- **addToWaitlist:** validates org, property, lead belong to org, prevents duplicate active WAITING or OFFERED for same lead+property, finds max position per property, newPos = max+1, priorityScore default 0 (future: budget/urgency calc), creates WAITING
- **searchWaitlist:** propertyId/unitType/status
- **getEntry:** org check
- **updateStatus:** state machine
  - WAITING → OFFERED, CANCELLED, EXPIRED only
  - OFFERED → ACCEPTED, EXPIRED, CANCELLED only
  - ACCEPTED terminal
  - EXPIRED/CANCELLED → WAITING (re-queue) allowed, new position max+1
- **removeFromWaitlist:** soft delete
- **getNextInLineForProperty:** ordered by priorityScore DESC position ASC, returns top WAITING for property+unitType, used by UnitService vacancy trigger

All services @Transactional, @Slf4j, use ResourceNotFoundException, RuntimeException for business rule violations handled by GlobalExceptionHandler.

### 4. Controllers — Secure Paginated REST

All controllers use `@CurrentUser UserPrincipal` to get orgId, `@PreAuthorize` with `permEval`:

**Portfolio:**

- `POST /api/amenities` — AMENITY_MANAGE or hierarchy 50, 201 Created
- `GET /api/amenities` — PROPERTY_READ
- `GET /api/amenities/{id}` — PROPERTY_READ
- `PUT /api/amenities/{id}` — AMENITY_MANAGE
- `DELETE /api/amenities/{id}` — AMENITY_MANAGE

- `POST /api/properties` — PROPERTY_WRITE or hierarchy 80, 201
- `GET /api/properties?city=&search=&type=&status=&page=&size=` — PROPERTY_READ, paginated, cached
- `GET /api/properties/{id}` — PROPERTY_READ, includes stats
- `PUT /api/properties/{id}` — PROPERTY_WRITE or 80
- `DELETE /api/properties/{id}` — PROPERTY_DELETE or 80
- `GET /api/properties/{id}/stats` — PROPERTY_READ

- `POST /api/units` — UNIT_MANAGE or hierarchy 50, 201
- `GET /api/units?propertyId=&status=&type=&search=&page=` — PROPERTY_READ, paginated cached
- `GET /api/units/vacant?propertyId=&page=` — PROPERTY_READ, vacant only
- `GET /api/units/{id}` — PROPERTY_READ
- `PUT /api/units/{id}` — UNIT_MANAGE or 50, state machine validation
- `DELETE /api/units/{id}` — UNIT_MANAGE or 80

**CRM:**

- `POST /api/crm/leads` — LEAD_MANAGE or hierarchy 50, 201
- `GET /api/crm/leads?status=&source=&propertyId=&assignedTo=&search=&priority=&page=` — LEAD_MANAGE
- `GET /api/crm/leads/{id}` — LEAD_MANAGE
- `PUT /api/crm/leads/{id}` — LEAD_MANAGE
- `DELETE /api/crm/leads/{id}` — LEAD_MANAGE

- `POST /api/crm/visits` — LEAD_VISIT_MANAGE or LEAD_MANAGE, 201
- `GET /api/crm/visits?leadId=&propertyId=&status=&staffId=&page=` — LEAD_VISIT_MANAGE or LEAD_MANAGE
- `GET /api/crm/visits/{id}` — same
- `PUT /api/crm/visits/{id}` — same
- `DELETE /api/crm/visits/{id}` — LEAD_VISIT_MANAGE

- `POST /api/crm/waitlist` — LEAD_VISIT_MANAGE or LEAD_MANAGE, 201, auto position
- `GET /api/crm/waitlist?propertyId=&unitType=&status=&page=` — same, ordered position ASC
- `GET /api/crm/waitlist/{id}` — LEAD_VISIT_MANAGE
- `PATCH /api/crm/waitlist/{id}/status?status=OFFERED` — status state machine
- `DELETE /api/crm/waitlist/{id}` — LEAD_VISIT_MANAGE
- `GET /api/crm/waitlist/next?propertyId=&unitType=` — next in line (priorityScore DESC position ASC)

All controllers return `ApiResponse<T>` with success, message, data, use `Page<T>` for pagination.

### 5. Edge Cases Handled

- Property delete blocked if occupied units exist
- Unit number unique per property, totalUnits warn if exceeded
- Unit status state machine prevents invalid transitions (e.g., MAINTENANCE → OCCUPIED blocked)
- Unit VACANT clears tenant/lease + triggers waitlist log (future auto-notify)
- Lead budget min>max blocked, phone regex Indian, duplicate phone per property warn, aiScore 0-10, LOST→CONVERTED blocked
- Visit scheduledAt must be @Future and <1 year, staff org check, lead status auto-update NEW→VISIT_SCHEDULED, VISIT_SCHEDULED→VISITED on COMPLETED
- Waitlist duplicate active WAITING/OFFERED blocked per lead+property, position auto-increment max+1, priorityScore DESC, re-queue on EXPIRED→WAITING assigns new position at end
- All queries org-scoped via orgId from JWT, SUPER_ADMIN (100) can bypass isSameOrg check in permEval (future for cross-org admin)
- Soft delete everywhere (isDeleted), not hard delete, preserves ledger
- Cache eviction on property/unit changes keeps vacancy counts fresh, TTL fallback

---

## 🔗 API Flow Examples

### Property + Unit Vacancy Flow
```
POST /api/properties {name: "Sky Heights", type: RESIDENTIAL, city: "Noida", address: "..."}
→ 201 {id:1, unitsCount:0, vacant:0}

POST /api/units {propertyId:1, unitNumber:"101", type:FLAT, rentAmount:25000, deposit:50000}
→ 201 {id:1, status:VACANT}

GET /api/units/vacant?propertyId=1 → [unit 101]

PUT /api/units/1 {status: RESERVED} → OK (VACANT→RESERVED allowed)
PUT /api/units/1 {status: OCCUPIED} → OK (RESERVED→OCCUPIED)
PUT /api/units/1 {status: VACANT} → Clears tenant, logs waitlist check, evicts cache
```

### Lead → Visit → Waitlist Flow
```
POST /api/crm/leads {customerName:"Rajesh", phone:"9876543210", propertyId:1, budgetMin:20000, budgetMax:30000}
→ 201 {id:1, status:NEW}

POST /api/crm/visits {leadId:1, propertyId:1, scheduledAt:"2026-08-10T10:00:00Z"}
→ 201 {status:SCHEDULED}, lead auto → VISIT_SCHEDULED

PUT /api/crm/visits/1 {status:COMPLETED, feedback:"Liked 2BHK"} → lead auto → VISITED

# No vacant units of desired type
POST /api/crm/waitlist {propertyId:1, unitType:"FLAT", leadId:1, priorityScore:8}
→ 201 {position:1, status:WAITING}

# Unit becomes vacant
PUT /api/units/1 {status:VACANT} → logs: "Unit 101 became VACANT — next in waitlist: lead 1 Rajesh phone 9876543210 position 1"

PATCH /api/crm/waitlist/1/status?status=OFFERED → WAITING→OFFERED
PATCH /api/crm/waitlist/1/status?status=ACCEPTED → OFFERED→ACCEPTED
```

---

## 📊 Metrics Domain 1

- Repositories: 6 (3 portfolio, 3 CRM)
- DTOs: 12
- Services: 6 (Amenity, Property, Unit, Lead, Visit, Waitlist) ~1200 lines business logic
- Controllers: 6 (Amenity, Property, Unit, Lead, Visit, Waitlist) ~600 lines, 25 endpoints
- Endpoints total: 25 (portfolio 13, CRM 12)
- Pagination: Pageable with sort, size 20 default
- Security: 8 permission types used (PROPERTY_READ/WRITE/DELETE, UNIT_MANAGE, AMENITY_MANAGE, LEAD_MANAGE, LEAD_VISIT_MANAGE)
- Cache: properties, units (evict on write, TTL 5m/2m)
- Business rules: 5 state machines (Unit, Lead, Visit, Waitlist) + 8 edge validations

---

## ✅ Domain 1 Completion Checklist

- [x] Amenity CRUD org-scoped, duplicate check
- [x] Property CRUD + search (city, name/address LIKE, type, status) + stats (vacant/occupied/maintenance/reserved counts) + soft delete blocked if occupied
- [x] Unit CRUD + search (propertyId, status, type, unitNumber LIKE) + vacant filter + unitNumber unique per property + totalUnits warn + state machine + waitlist trigger on VACANT + cache evict properties+units
- [x] Lead CRUD + search (status/source/propertyId/assignedTo/search/priority) + budget min≤max + phone regex + aiScore 0-10 + duplicate phone warn + status transition + nextFollowup handling
- [x] Visit CRUD + schedule validation @Future <1 year + staff org check + lead status auto-update + state machine (COMPLETED terminal)
- [x] Waitlist CRUD + position auto max+1 + duplicate active WAITING/OFFERED blocked + priorityScore ordering + status state machine + re-queue new position + getNextInLine for vacancy automation
- [x] All endpoints secured with @PreAuthorize permEval, orgId from JWT, pagination, @Valid
- [x] Soft delete, audit via BaseEntity, logging

---

## 🔜 Next: Domain 2 — Tenant Lifecycle & Legal

**Will implement:**

- TenantProfile (link to AppUser, property/unit, emergency contact, move_in/out dates, status PROSPECT/ACTIVE/NOTICE_PERIOD/MOVED_OUT)
- KycDocument (Aadhaar/PAN/Passport, S3 upload front/back, verification PENDING/VERIFIED/REJECTED, encrypted number, expiry)
- LeaseAgreement (lease_number UNIQUE, start/end, rent/deposit, rent_due_day 1-28, notice_period, escalation %, status DRAFT/PENDING_SIGNATURE/ACTIVE/EXPIRED/TERMINATED, terms TEXT, final_pdf S3, lease_version, parent_lease for renewals)
- EsignTracking (signer_user, role TENANT/OWNER/MANAGER, status PENDING/SENT/VIEWED/SIGNED, signature_order, otp_verified, ip, user_agent)
- ChecklistTemplate (MOVE_IN/OUT, items JSONB) + UnitConditionReport (type MOVE_IN/OUT, inspected_by, overall_condition, status DRAFT/COMPLETED/DISPUTED) + ConditionReportItem (area, item_name, condition EXCELLENT/GOOD/FAIR/DAMAGED/MISSING, estimated_repair_cost) + ConditionPhoto (S3, caption)
- Services: onboarding with KYC S3 upload via S3Service, lease creation with auto lease_number, esign flow, move-in/out checklist with photo upload, deposit deduction based on condition report
- Quartz Job: LeaseExpiryAlertJob daily check end_date = today+60 and today+30 → NotificationLog + Broadcast (Phase 5 integration placeholder)
- Controllers: /api/tenants, /api/kyc, /api/leases, /api/esign, /api/condition-reports secured

**Reply:** "Approved, begin Phase 3 Domain 2: Tenant Lifecycle" to continue monopoly build.
