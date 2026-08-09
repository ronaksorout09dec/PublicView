package com.skyheights.realestate.common.seeder;

import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.organization.entity.Permission;
import com.skyheights.realestate.modules.organization.entity.Role;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.UserRole;
import com.skyheights.realestate.modules.organization.enums.RoleName;
import com.skyheights.realestate.modules.organization.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Prop-OS Phase 2 Seeder — Initializes Permissions, System Roles, Super Admin
 * Runs on startup, idempotent
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RolePermissionSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    // 31 Permissions — Enterprise RBAC
    private static final List<PermissionDef> PERMISSIONS = List.of(
            new PermissionDef("PROPERTY_READ", "View properties", "PORTFOLIO"),
            new PermissionDef("PROPERTY_WRITE", "Create/Edit properties", "PORTFOLIO"),
            new PermissionDef("PROPERTY_DELETE", "Delete properties", "PORTFOLIO"),
            new PermissionDef("UNIT_MANAGE", "Manage units", "PORTFOLIO"),
            new PermissionDef("AMENITY_MANAGE", "Manage amenities", "PORTFOLIO"),
            new PermissionDef("LEAD_MANAGE", "Manage CRM leads", "CRM"),
            new PermissionDef("LEAD_VISIT_MANAGE", "Manage visits & waitlist", "CRM"),
            new PermissionDef("TENANT_READ", "View tenants", "TENANT"),
            new PermissionDef("TENANT_WRITE", "Create/Edit tenants", "TENANT"),
            new PermissionDef("KYC_VERIFY", "Verify KYC docs", "TENANT"),
            new PermissionDef("LEASE_MANAGE", "Manage leases", "TENANT"),
            new PermissionDef("LEASE_ESIGN", "Manage e-sign", "TENANT"),
            new PermissionDef("INVOICE_MANAGE", "Manage invoices", "FINANCIAL"),
            new PermissionDef("INVOICE_VIEW", "View invoices", "FINANCIAL"),
            new PermissionDef("LATE_FEE_MANAGE", "Manage late fee rules", "FINANCIAL"),
            new PermissionDef("UTILITY_MANAGE", "Manage utility meters & bills", "FINANCIAL"),
            new PermissionDef("DEPOSIT_MANAGE", "Manage security deposits", "FINANCIAL"),
            new PermissionDef("TRANSACTION_MANAGE", "Manage transactions", "FINANCIAL"),
            new PermissionDef("REPORT_VIEW", "View tax/account reports", "FINANCIAL"),
            new PermissionDef("TICKET_CREATE", "Create tickets (tenant)", "MAINTENANCE"),
            new PermissionDef("TICKET_MANAGE", "Manage all tickets", "MAINTENANCE"),
            new PermissionDef("VENDOR_MANAGE", "Manage vendors", "MAINTENANCE"),
            new PermissionDef("VENDOR_BID", "Submit/view bids (vendor)", "MAINTENANCE"),
            new PermissionDef("VENDOR_PAYOUT_MANAGE", "Manage payouts", "MAINTENANCE"),
            new PermissionDef("COMMUNICATION_SEND", "Send notifications/broadcasts", "COMMUNICATION"),
            new PermissionDef("COMMUNICATION_TEMPLATE_MANAGE", "Manage templates & automation", "COMMUNICATION"),
            new PermissionDef("IOT_MANAGE", "Manage smart locks & pins", "IOT"),
            new PermissionDef("IOT_PIN_GENERATE", "Generate temporary PINs", "IOT"),
            new PermissionDef("USER_MANAGE", "Manage users & roles", "ADMIN"),
            new PermissionDef("SETTINGS_MANAGE", "Manage org settings", "ADMIN"),
            new PermissionDef("ORG_MANAGE", "Manage organizations (super admin)", "ADMIN")
    );

    // Role -> Permissions mapping
    private static final Map<RoleName, List<String>> ROLE_PERMISSIONS = Map.of(
            RoleName.SUPER_ADMIN, PERMISSIONS.stream().map(p -> p.name).collect(Collectors.toList()),
            RoleName.PROPERTY_MANAGER, List.of(
                    "PROPERTY_READ","PROPERTY_WRITE","PROPERTY_DELETE","UNIT_MANAGE","AMENITY_MANAGE",
                    "LEAD_MANAGE","LEAD_VISIT_MANAGE","TENANT_READ","TENANT_WRITE","KYC_VERIFY",
                    "LEASE_MANAGE","LEASE_ESIGN","INVOICE_MANAGE","INVOICE_VIEW","LATE_FEE_MANAGE",
                    "UTILITY_MANAGE","DEPOSIT_MANAGE","TRANSACTION_MANAGE","REPORT_VIEW",
                    "TICKET_MANAGE","VENDOR_MANAGE","VENDOR_PAYOUT_MANAGE",
                    "COMMUNICATION_SEND","COMMUNICATION_TEMPLATE_MANAGE","IOT_MANAGE","IOT_PIN_GENERATE",
                    "USER_MANAGE","SETTINGS_MANAGE"
            ),
            RoleName.ACCOUNTANT, List.of(
                    "PROPERTY_READ","UNIT_MANAGE","TENANT_READ","INVOICE_MANAGE","INVOICE_VIEW",
                    "LATE_FEE_MANAGE","UTILITY_MANAGE","DEPOSIT_MANAGE","TRANSACTION_MANAGE","REPORT_VIEW",
                    "VENDOR_PAYOUT_MANAGE"
            ),
            RoleName.STAFF, List.of(
                    "PROPERTY_READ","UNIT_MANAGE","LEAD_MANAGE","LEAD_VISIT_MANAGE","TENANT_READ","TENANT_WRITE",
                    "LEASE_MANAGE","INVOICE_VIEW","TICKET_MANAGE","VENDOR_MANAGE","COMMUNICATION_SEND"
            ),
            RoleName.LEAD_AGENT, List.of(
                    "PROPERTY_READ","LEAD_MANAGE","LEAD_VISIT_MANAGE","TENANT_READ","COMMUNICATION_SEND"
            ),
            RoleName.TENANT, List.of(
                    "PROPERTY_READ","TICKET_CREATE","INVOICE_VIEW"
            ),
            RoleName.VENDOR, List.of(
                    "TICKET_CREATE","VENDOR_BID","PROPERTY_READ"
            )
    );

    private static final Map<RoleName, Integer> ROLE_HIERARCHY = Map.of(
            RoleName.SUPER_ADMIN, 100,
            RoleName.PROPERTY_MANAGER, 80,
            RoleName.ACCOUNTANT, 60,
            RoleName.LEAD_AGENT, 51,
            RoleName.STAFF, 50,
            RoleName.TENANT, 30,
            RoleName.VENDOR, 20
    );

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Prop-OS Seeder: Starting Role/Permission seeding ===");

        // 1. Seed Permissions
        Map<String, Permission> permMap = new HashMap<>();
        for (PermissionDef def : PERMISSIONS) {
            Permission perm = permissionRepository.findByName(def.name)
                    .orElseGet(() -> {
                        log.info("Creating permission {}", def.name);
                        return permissionRepository.save(Permission.builder()
                                .name(def.name)
                                .description(def.description)
                                .category(def.category)
                                .build());
                    });
            permMap.put(def.name, perm);
        }
        log.info("Seeded {} permissions", permMap.size());

        // 2. Seed System Roles (org_id = NULL)
        for (RoleName roleName : RoleName.values()) {
            Role role = roleRepository.findByNameAndOrgIdIsNull(roleName)
                    .orElseGet(() -> {
                        log.info("Creating system role {}", roleName);
                        Role newRole = Role.builder()
                                .name(roleName)
                                .description(getRoleDescription(roleName))
                                .hierarchyLevel(ROLE_HIERARCHY.getOrDefault(roleName, 0))
                                .isSystem(true)
                                .build();
                        return roleRepository.save(newRole);
                    });

            // Assign permissions to role
            List<String> permNames = ROLE_PERMISSIONS.getOrDefault(roleName, List.of());
            Set<Permission> perms = permNames.stream()
                    .map(permMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (role.getPermissions() == null || role.getPermissions().size() != perms.size()) {
                role.setPermissions(perms);
                roleRepository.save(role);
                log.info("Assigned {} permissions to role {}", perms.size(), roleName);
            }
            // Ensure hierarchy correct
            if (!Objects.equals(role.getHierarchyLevel(), ROLE_HIERARCHY.get(roleName))) {
                role.setHierarchyLevel(ROLE_HIERARCHY.get(roleName));
                roleRepository.save(role);
            }
        }
        log.info("Seeded {} system roles", RoleName.values().length);

        // 3. Seed Default Organization + Super Admin for dev
        seedSuperAdmin(permMap);

        log.info("=== Prop-OS Seeder: Complete ===");
    }

    private void seedSuperAdmin(Map<String, Permission> permMap) {
        String superAdminEmail = "superadmin@propos.io";
        String defaultOrgSlug = "propos-platform";

        Organization platformOrg = organizationRepository.findBySlug(defaultOrgSlug)
                .orElseGet(() -> {
                    log.info("Creating platform organization {}", defaultOrgSlug);
                    Organization org = Organization.builder()
                            .name("Prop-OS Platform")
                            .slug(defaultOrgSlug)
                            .billingEmail(superAdminEmail)
                            .build();
                    return organizationRepository.save(org);
                });

        AppUser superAdmin = appUserRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(superAdminEmail)
                .orElseGet(() -> {
                    log.info("Creating Super Admin user {}", superAdminEmail);
                    AppUser user = AppUser.builder()
                            .email(superAdminEmail)
                            .passwordHash(passwordEncoder.encode("SuperAdmin123!"))
                            .fullName("Prop-OS Super Admin")
                            .phone("9999999999")
                            .organization(platformOrg)
                            .build();
                    return appUserRepository.save(user);
                });

        // Assign SUPER_ADMIN role
        Role superAdminRole = roleRepository.findByNameAndOrgIdIsNull(RoleName.SUPER_ADMIN)
                .orElseThrow();

        boolean hasRole = userRoleRepository.findByUserId(superAdmin.getId()).stream()
                .anyMatch(ur -> ur.getRole().getName() == RoleName.SUPER_ADMIN);

        if (!hasRole) {
            UserRole ur = UserRole.builder()
                    .user(superAdmin)
                    .role(superAdminRole)
                    .organization(platformOrg)
                    .build();
            userRoleRepository.save(ur);
            log.info("Assigned SUPER_ADMIN role to {}", superAdminEmail);
        }

        // Update org owner if not set
        if (platformOrg.getOwnerUserId() == null) {
            platformOrg.setOwnerUserId(superAdmin.getId());
            organizationRepository.save(platformOrg);
        }

        // Also seed demo PROPERTY_MANAGER org for ease of testing
        String demoManagerEmail = "manager@demo.com";
        String demoOrgSlug = "demo-estates";
        Organization demoOrg = organizationRepository.findBySlug(demoOrgSlug)
                .orElseGet(() -> {
                    log.info("Creating demo org {}", demoOrgSlug);
                    Organization org = Organization.builder()
                            .name("Demo Estates")
                            .slug(demoOrgSlug)
                            .billingEmail(demoManagerEmail)
                            .build();
                    return organizationRepository.save(org);
                });

        AppUser demoManager = appUserRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(demoManagerEmail)
                .orElseGet(() -> {
                    log.info("Creating demo manager {}", demoManagerEmail);
                    AppUser user = AppUser.builder()
                            .email(demoManagerEmail)
                            .passwordHash(passwordEncoder.encode("Manager123!"))
                            .fullName("Demo Property Manager")
                            .phone("9876543210")
                            .organization(demoOrg)
                            .build();
                    return appUserRepository.save(user);
                });

        Role managerRole = roleRepository.findByNameAndOrgIdIsNull(RoleName.PROPERTY_MANAGER).orElseThrow();
        boolean managerHasRole = userRoleRepository.findByUserId(demoManager.getId()).stream()
                .anyMatch(ur -> ur.getRole().getName() == RoleName.PROPERTY_MANAGER);
        if (!managerHasRole) {
            userRoleRepository.save(UserRole.builder()
                    .user(demoManager)
                    .role(managerRole)
                    .organization(demoOrg)
                    .build());
            log.info("Assigned PROPERTY_MANAGER to {}", demoManagerEmail);
        }

        if (demoOrg.getOwnerUserId() == null) {
            demoOrg.setOwnerUserId(demoManager.getId());
            organizationRepository.save(demoOrg);
        }

        log.info("Default credentials: superadmin@propos.io / SuperAdmin123! | manager@demo.com / Manager123!");
    }

    private String getRoleDescription(RoleName roleName) {
        return switch (roleName) {
            case SUPER_ADMIN -> "Platform super admin - all access, hierarchy 100";
            case PROPERTY_MANAGER -> "Property tycoon / org admin - hierarchy 80";
            case ACCOUNTANT -> "Financial role - income/expense/tax - 60";
            case LEAD_AGENT -> "CRM agent - leads/visits - 51";
            case STAFF -> "Manager's team - limited ops - 50";
            case TENANT -> "Tenant self-service - 30";
            case VENDOR -> "Vendor - ticket bidding - 20";
        };
    }

    private record PermissionDef(String name, String description, String category) {}
}
