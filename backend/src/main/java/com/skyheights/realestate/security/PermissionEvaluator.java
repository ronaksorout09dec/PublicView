package com.skyheights.realestate.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Enterprise Permission Evaluator for Prop-OS
 * Supports hierarchical RBAC + permission checks
 *
 * Usage in @PreAuthorize:
 * @PreAuthorize("@permEval.hasPermission('PROPERTY_WRITE')")
 * @PreAuthorize("@permEval.hasHierarchy(80)") // requires at least PROPERTY_MANAGER
 * @PreAuthorize("@permEval.canActOn(50)") // can act on STAFF (50) if higher
 * @PreAuthorize("@permEval.isSameOrg(#orgId)")
 */
@Component("permEval")
@RequiredArgsConstructor
@Slf4j
public class PermissionEvaluator {

    public boolean hasPermission(String permission) {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        boolean result = principal.hasPermission(permission);
        log.debug("Permission check {} for user {} => {}", permission, principal.getUsername(), result);
        return result;
    }

    public boolean hasRole(String role) {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        // Accept both ROLE_XXX and XXX
        String normalized = role.startsWith("ROLE_") ? role.substring(5) : role;
        return principal.hasRole(normalized);
    }

    public boolean hasHierarchy(int minimumLevel) {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        boolean result = principal.hasHierarchyLevelAtLeast(minimumLevel);
        log.debug("Hierarchy check >= {} for user {} (level {}) => {}", minimumLevel, principal.getUsername(), principal.getMaxHierarchyLevel(), result);
        return result;
    }

    public boolean canActOn(int targetLevel) {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        boolean result = principal.canActOn(targetLevel);
        log.debug("CanActOn check target {} for user {} (level {}) => {}", targetLevel, principal.getUsername(), principal.getMaxHierarchyLevel(), result);
        return result;
    }

    public boolean isSameOrg(Long orgId) {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        // SUPER_ADMIN (100) can cross org
        if (principal.getMaxHierarchyLevel() >= 100) return true;
        if (principal.getOrgId() == null) return false;
        return principal.getOrgId().equals(orgId);
    }

    public boolean isOwnerOrSuperAdmin(Long ownerUserId) {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        if (principal.getMaxHierarchyLevel() >= 100) return true;
        return principal.getId().equals(ownerUserId);
    }
}
