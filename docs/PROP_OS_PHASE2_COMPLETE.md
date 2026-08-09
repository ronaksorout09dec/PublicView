# Prop-OS Phase 2 — COMPLETE ✅
### Project Initialization & Core Security (Enterprise RBAC + JWT + Redis + S3)

**Date:** Aug 08, 2026  
**Branch:** arena/019fe27f-publicview  
**Status:** Security backbone operational → Ready for Phase 3 Domain Implementation

---

## 📦 Deliverables

### 1. Pom.xml Upgraded — Enterprise Stack

**File:** `backend/pom.xml` (3.2.5 → Prop-OS stack)

**Added Dependencies:**
- `spring-boot-starter-security` — Security filter chain, BCrypt(12), MethodSecurity
- `spring-boot-starter-data-redis` + `spring-boot-starter-cache` — Lettuce client, RedisTemplate, CacheManager (properties, units, tenants, invoices, accessPins TTL)
- `flyway-core` + `flyway-database-postgresql` — Production migrations (disabled for H2 dev, enabled via FLYWAY_ENABLED=true)
- `jjwt-api 0.12.5` + `jjwt-impl` + `jjwt-jackson` — JWT creation/validation, claims parsing
- `software.amazon.awssdk:s3,auth,regions 2.25.27` — S3Client + S3Presigner for presigned URLs, path `{org_id}/{entity}/{year}/{uuid}-{filename}`
- `hibernate-types-60 2.21.1` — Additional JSONB support
- `embedded-redis` + `spring-security-test` for test scope

**Java 21, Spring Boot 3.2.5 retained**

### 2. Application Configuration

**File:** `backend/src/main/resources/application.properties` (60 → 120 lines)

- Database: `DATABASE_URL` with H2 fallback `MODE=PostgreSQL`, `DDL_AUTO` env, Flyway toggle
- JWT: `app.jwt.secret` Base64 (dev default 64+ bytes), `expiration-ms 900000 (15m)`, `refresh-expiration-ms 604800000 (7d)`
- Redis: `spring.data.redis.host/port/password`, lettuce pool max-active 20, `spring.cache.type=simple` default (redis for prod)
- S3: `aws.s3.enabled=false` dev, `region`, `bucket`, `accessKeyId/secretAccessKey`, presigner
- CORS: `*` patterns for preview hosts `https://*.e2b.app`
- Legacy: Ollama Qwen3 still supported `/api/voice/*`
- Logging: security DEBUG, hibernate WARN

**File:** `backend/.env.example` updated with all new env vars

**Docker:** `docker-compose.yml` updated
- `propos-postgres` → DB `propos`
- NEW `propos-redis` 7-alpine with persistence + healthcheck
- `propos-backend` env: JWT, Redis host=redis, CACHE_TYPE=redis, S3 disabled dev
- Renamed containers `skyheights-*` → `propos-*`

### 3. Core Security — Enterprise RBAC

#### 3.1 Entities & Repositories (already from Phase 1 + new repos)

**Repositories:**
- `modules/organization/repository/OrganizationRepository` — findBySlug, existsBySlug, isDeleted filter
- `AppUserRepository` — findFirstByEmailIgnoreCaseOrderByCreatedAtDesc (multi-tenant same email), findByOrgIdAndIsDeletedFalse, existsByEmailAndOrgId
- `RoleRepository` — findSystemRoleWithPermissions (JOIN FETCH), findByNameAndOrgIdIsNull, findByOrgId
- `PermissionRepository` — findByName
- `UserRoleRepository` — findByUserIdWithRoleAndPermissions (JOIN FETCH role + permissions), findByUserIdAndOrgIdWithRole

#### 3.2 Security Components

**`security/UserPrincipal.java` — UserDetails:**
- Fields: id, uuid, orgId, orgSlug, email, fullName, authorities (ROLE_XXX + permission strings), maxHierarchyLevel, roles List, permissions List, active
- `create(AppUser, List<Role>)` — extracts permissions flat, max hierarchy, builds GrantedAuthority list
- Methods: `hasPermission(perm)`, `hasRole(role)`, `hasHierarchyLevelAtLeast(level)`, `canActOn(targetLevel)` — hierarchical logic: level > target or SUPER_ADMIN (100)

**`security/CustomUserDetailsService.java`:**
- `loadUserByUsername(email)` — finds first by email desc, checks isDeleted, loads UserRole with permissions via custom query
- `loadUserById(id)` — for JWT filter

**`security/JwtTokenProvider.java` — JJWT 0.12.5:**
- `init()` — Decodes Base64 secret → SecretKey (fallback generates HS512 key if invalid)
- `generateToken(Authentication)` — subject=email, claims: userId, uuid, orgId, orgSlug, fullName, roles, permissions, hierarchyLevel, iat, exp 15m, signWith key
- `generateRefreshToken()` — type=refresh, exp 7d
- `validateToken()` — handles SecurityException, Malformed, Expired, Unsupported, IllegalArgument
- `parseClaims()`, `getUserIdFromJWT()`, `getUsernameFromJWT()`

**`security/JwtAuthenticationFilter.java` — OncePerRequestFilter:**
- Extracts `Authorization: Bearer <token>` → validate → getUserId → loadUserById → set SecurityContext authentication
- Logs debug set authentication

**`security/JwtAuthenticationEntryPoint.java`:**
- Returns 401 JSON with success=false, error=Unauthorized, timestamp, path

**`security/SecurityConfig.java`:**
- `@EnableWebSecurity` + `@EnableMethodSecurity(prePostEnabled=true, securedEnabled=true)`
- PasswordEncoder BCrypt(12)
- DaoAuthenticationProvider with CustomUserDetailsService
- CorsConfigurationSource: AllowedOriginPatterns *, methods GET/POST/PUT/DELETE/OPTIONS/PATCH, headers *, credentials true, maxAge 3600
- FilterChain: CORS, CSRF disable, exceptionHandling→unauthorizedHandler, SessionCreationPolicy.STATELESS, authorizeHttpRequests:
  - permitAll: `/api/auth/**`, `/api/health`, `/actuator/**`, `/api/voice/**` (legacy), `/api/call-summary`, `/error`, `/api/leads/**` (legacy open for Phase 2)
  - anyRequest authenticated
- Adds JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter, InputSanitizationFilter after JWT

**`security/PermissionEvaluator.java` — Bean name `permEval`:**
- Methods for @PreAuthorize SpEL:
  - `hasPermission('PROPERTY_WRITE')` — checks principal.permissions
  - `hasRole('PROPERTY_MANAGER')` — normalizes ROLE_ prefix
  - `hasHierarchy(80)` — at least PROPERTY_MANAGER level
  - `canActOn(50)` — can act on STAFF (50) if higher or SUPER_ADMIN
  - `isSameOrg(orgId)` — SUPER_ADMIN cross-org, else orgId equality
  - `isOwnerOrSuperAdmin(ownerUserId)` — owner check
- Usage: `@PreAuthorize("@permEval.hasPermission('USER_MANAGE') or @permEval.hasHierarchy(80)")`

**`security/CurrentUser.java` — Meta-annotation:**
- `@AuthenticationPrincipal` wrapper for clean controller injection `@CurrentUser UserPrincipal currentUser`

**`exception/GlobalExceptionHandler.java` extended:**
- BadCredentialsException → 401 BAD_CREDENTIALS
- AccessDeniedException → 403 FORBIDDEN with message about permission/hierarchy
- RuntimeException → 404 if contains not found else 400

#### 3.3 Controllers & DTOs & Services

**DTOs:**
- `LoginRequest` — @Email, @NotBlank password, optional orgSlug for multi-tenant login
- `RegisterRequest` — fullName, @Email, @Size 8-20 password, phone, orgName, orgSlug, role (default PROPERTY_MANAGER)
- `JwtResponse` — accessToken, refreshToken, tokenType Bearer, userId, uuid, email, fullName, orgId, orgSlug, orgName, roles, permissions, hierarchyLevel
- `UserResponse` — id, uuid, email, fullName, phone, orgId, orgSlug, status, roles, permissions, hierarchyLevel, createdAt, lastLogin

**`modules/organization/service/AuthService.java`:**
- `login(LoginRequest)` — authenticate via AuthenticationManager → generate JWT + refresh → update lastLogin → return JwtResponse with org name
- `register(RegisterRequest)` — check email exists in org scope, find or create org (slug generation via name + millis), create AppUser with BCrypt, assign Role (system role), create UserRole, set org owner if PROPERTY_MANAGER first, then auto-login via login()
- `getCurrentUser(userId)` — loads user + roles + perms + maxLevel → UserResponse
- `canActOn(actorUserId, targetLevel)` — hierarchical check

**`modules/organization/controller/AuthController.java`:**
- `POST /api/auth/login` — public, returns JwtResponse
- `POST /api/auth/register` — public, creates org+user+role, returns JWT
- `GET /api/auth/me` — secured, @CurrentUser UserPrincipal, returns UserResponse
- `POST /api/auth/refresh` — placeholder for refresh flow (client should re-login for Phase 2)

**`modules/organization/controller/OrganizationController.java`:**
- `GET /api/organizations` — @PreAuthorize hasHierarchy(80) or ORG_MANAGE, SUPER_ADMIN sees all, others see own org only
- `GET /api/organizations/page` — paginated, requires ORG_MANAGE
- `GET /api/organizations/{id}` — isSameOrg or ORG_MANAGE
- `POST /api/organizations` — requires ORG_MANAGE, creates org with slug check
- Demonstrates hierarchical + permission SpEL usage

**`modules/organization/controller/UserController.java`:**
- `GET /api/users` — hasPermission USER_MANAGE or hierarchy 80, optional orgId param, SUPER_ADMIN can see all if orgId null else org-scoped
- `GET /api/users/{id}` — USER_MANAGE or hierarchy 50, checks same org unless SUPER_ADMIN
- Returns UserResponse with roles/permissions/hierarchy

#### 3.4 Infrastructure Config

**`common/config/AuditingConfig.java`:**
- `@EnableJpaAuditing` + AuditorAware<Long> bean — extracts current UserPrincipal id from SecurityContext, returns Optional userId for createdBy/updatedBy in BaseEntity

**`common/config/RedisConfig.java`:**
- `@EnableCaching`, LettuceConnectionFactory with host/port log, RedisTemplate String/Object with Jackson2Json serializer
- CacheManager @ConditionalOnProperty spring.cache.type=redis — 5 caches with TTLs: properties 5m, units 2m, tenants 5m, invoices 1m, accessPins 10m
- Default cache type simple for H2 dev, redis for prod

**`common/config/S3Config.java`:**
- S3Client bean with dummy creds fallback when disabled, logs warn, region from env
- S3Presigner bean similarly with dummy creds
- No null beans — always returns client to avoid NPE, service checks enabled flag

**`common/service/S3Service.java`:**
- `generateKey(orgId, entity, filename)` → `{org_id}/{entity}/{year}/{uuid}-{filename}`
- `uploadFile(key, InputStream, length, contentType)` — mocked if disabled (returns key, logs warn), else PutObjectRequest
- `generatePresignedUrl(key, Duration)` — mocked URL if disabled else presign
- `isEnabled()`, `getBucket()` helpers for future domains

**`common/seeder/RolePermissionSeeder.java` — CommandLineRunner Order(1):**
- Idempotent seeding on startup
- 31 Permissions: PROPERTY_READ/WRITE/DELETE, UNIT_MANAGE, AMENITY_MANAGE, LEAD_MANAGE, LEAD_VISIT_MANAGE, TENANT_READ/WRITE, KYC_VERIFY, LEASE_MANAGE, LEASE_ESIGN, INVOICE_MANAGE/VIEW, LATE_FEE_MANAGE, UTILITY_MANAGE, DEPOSIT_MANAGE, TRANSACTION_MANAGE, REPORT_VIEW, TICKET_CREATE/MANAGE, VENDOR_MANAGE/BID/PAYOUT_MANAGE, COMMUNICATION_SEND/TEMPLATE_MANAGE, IOT_MANAGE/PIN_GENERATE, USER_MANAGE, SETTINGS_MANAGE, ORG_MANAGE
- Role → Permissions mapping:
  - SUPER_ADMIN (100) → all 31
  - PROPERTY_MANAGER (80) → 28 perms (all except ORG_MANAGE etc)
  - ACCOUNTANT (60) → 11 financial perms
  - LEAD_AGENT (51) → 5 CRM perms
  - STAFF (50) → 11 limited ops
  - TENANT (30) → PROPERTY_READ, TICKET_CREATE, INVOICE_VIEW
  - VENDOR (20) → TICKET_CREATE, VENDOR_BID, PROPERTY_READ
- Hierarchy map: 100,80,60,51,50,30,20
- Seeds system roles org_id NULL with permissions
- Seeds platform org `propos-platform` + superadmin `superadmin@propos.io / SuperAdmin123!` with SUPER_ADMIN role
- Seeds demo org `demo-estates` + manager `manager@demo.com / Manager123!` with PROPERTY_MANAGER role
- Logs default credentials

**`RealEstateApplication.java` updated:**
- @EnableCaching, @EnableJpaAuditing, @EnableScheduling
- Logs Prop-OS banner with phases, default creds, APIs

---

## 🔐 RBAC Hierarchy Model

```
SUPER_ADMIN (100) — Platform owner, cross-org, ORG_MANAGE
    |
PROPERTY_MANAGER (80) — Tycoon, org admin, can manage staff/tenants/vendors, USER_MANAGE, SETTINGS_MANAGE
    |
ACCOUNTANT (60) — Financial, TRANSACTION_MANAGE, REPORT_VIEW
    |
LEAD_AGENT (51) — CRM, LEAD_MANAGE
    |
STAFF (50) — Limited ops, PROPERTY_READ, TICKET_MANAGE, VENDOR_MANAGE
    |
TENANT (30) — Self-service, TICKET_CREATE, INVOICE_VIEW
    |
VENDOR (20) — Bidding only, VENDOR_BID
```

**Hierarchical check:** `canActOn` → actor level > target level OR actor == SUPER_ADMIN. Example: PROPERTY_MANAGER (80) can act on STAFF (50) but not on SUPER_ADMIN (100).

**Permission check:** User may have multiple roles, permissions aggregated flat distinct. Example: PROPERTY_MANAGER has PROPERTY_WRITE, so can create property; STAFF without PROPERTY_WRITE cannot.

**Org isolation:** `isSameOrg(orgId)` → SUPER_ADMIN bypass, else orgId equality. All tenant data filtered by org_id in service layer (Phase 3 will add Hibernate filter).

---

## 🚀 How to Run (Phase 2)

### Local with H2 + Simple Cache (no Redis/Postgres needed)
```bash
cd backend
export DATABASE_URL=jdbc:h2:mem:realestate
mvn spring-boot:run
# App starts on 8080, seeds roles/permissions + superadmin + demo manager
```

### With Docker (Postgres + Redis + Ollama)
```bash
docker-compose up --build -d
# Wait 15s
curl http://localhost:8080/api/health
```

### Test Auth Flow
```bash
# Register new tycoon org
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Ratan Tata Estates","email":"ratan@tata.com","password":"Tata123!@#","orgName":"Tata Estates","role":"PROPERTY_MANAGER"}'

# Login superadmin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"superadmin@propos.io","password":"SuperAdmin123!"}'

# Response: accessToken, refreshToken, orgId, roles, permissions, hierarchyLevel

# Use token
TOKEN=eyJ...
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"

# Organizations (SUPER_ADMIN only)
curl http://localhost:8080/api/organizations -H "Authorization: Bearer $TOKEN"

# Users in org
curl http://localhost:8080/api/users -H "Authorization: Bearer $TOKEN"
```

---

## 📊 Metrics Phase 2

- Dependencies added: 8 (security, redis, cache, flyway, jjwt 3 artifacts, s3 3 artifacts)
- Security classes: 6 (UserPrincipal, CustomUserDetailsService, JwtTokenProvider, JwtAuthenticationFilter, JwtAuthenticationEntryPoint, SecurityConfig)
- Repositories: 5 (Org, AppUser, Role, Permission, UserRole)
- DTOs: 4 (Login, Register, JwtResponse, UserResponse)
- Services: 1 AuthService + S3Service + PermissionEvaluator
- Controllers: 3 (Auth, Organization, User)
- Config: 4 (Auditing, Redis, S3, Security)
- Seeder: 1 RolePermissionSeeder (31 perms, 7 roles, 2 orgs, 2 users)
- Lines: ~2500 new

---

## ✅ Phase 2 Completion Checklist

- [x] pom.xml upgraded with Security, JWT, Redis, S3, Flyway
- [x] application.properties with JWT, Redis, S3, Flyway, CORS enterprise
- [x] BaseEntity + AuditingConfig (createdBy/updatedBy from SecurityContext)
- [x] RBAC hierarchical: Role hierarchyLevel 100>80>60>51>50>30>20
- [x] 31 Permissions seeded + 7 System Roles with mapping
- [x] JWT generation 15m access + 7d refresh, validation, claims (userId, orgId, roles, perms, hierarchy)
- [x] JwtAuthenticationFilter + EntryPoint + SecurityConfig stateless
- [x] Super Admin + Demo Manager seeded with credentials logged
- [x] Auth endpoints /api/auth/login, /register, /me working
- [x] Organization/User controllers with @PreAuthorize permEval SpEL
- [x] RedisConfig with TTL caches, conditional on cache type
- [x] S3Config + S3Service placeholder with org-isolated key generation
- [x] docker-compose.yml with postgres + redis + backend + ollama
- [x] GlobalExceptionHandler extended for BadCredentials, AccessDenied

---

## 🔜 Next: Phase 3 — Domain Implementation (Iterative)

**You will pick a domain each time (I recommend order):**

1. **Core Portfolio & CRM** — Property, Unit, Amenity, CrmLead, Visit, Waitlist + paginated CRUD + vacancy logic
2. **Tenant Lifecycle & Legal** — TenantProfile, KYC S3 upload, Lease + eSign, Condition Reports S3, 60/30 day alerts
3. **Financial Engine** — Invoice auto-generation 1st of month, late-fee calc, utility split ratio/submeter, deposit ledger, tax report PDF S3
4. **Maintenance & Vendor Bidding** — Ticket photo S3, broadcast to vendors, bid submit, approve state machine, payout
5. **Communication & Automation** — Templates with {{variables}}, WhatsApp/SMS/Email mock, Broadcast, AutomationRule trigger, Redis queue
6. **IoT & Smart Locks** — Device registry, PIN generation encrypted, TTL Redis, AccessLog, webhook

Each domain will have:
- JPA Repositories (with org_id filtering)
- Services with business logic + edge cases (mid-month proration, overpayment credit, etc.)
- REST Controllers secured paginated (@PreAuthorize)
- DTOs + @Valid validation
- S3 upload where needed, Redis caching, Quartz jobs

**Reply:** "Approved, begin Phase 3 Domain 1: Portfolio & CRM" to start iterative domain build.

---

*Generated by Elite Enterprise Architect — Prop-OS Phase 2 Complete*
