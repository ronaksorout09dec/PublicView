package com.skyheights.realestate.security;

import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enterprise UserPrincipal for Prop-OS
 * Holds user, org, roles, permissions, hierarchyLevel for RBAC evaluation
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String uuid;
    private final Long orgId;
    private final String orgSlug;
    private final String email;
    private final String password;
    private final String fullName;
    private final Collection<? extends GrantedAuthority> authorities;
    private final int maxHierarchyLevel; // highest role level e.g SUPER_ADMIN=100
    private final List<String> roles;
    private final List<String> permissions;
    private final boolean active;

    public UserPrincipal(Long id, String uuid, Long orgId, String orgSlug, String email,
                         String password, String fullName,
                         Collection<? extends GrantedAuthority> authorities,
                         int maxHierarchyLevel, List<String> roles, List<String> permissions, boolean active) {
        this.id = id;
        this.uuid = uuid;
        this.orgId = orgId;
        this.orgSlug = orgSlug;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.authorities = authorities;
        this.maxHierarchyLevel = maxHierarchyLevel;
        this.roles = roles;
        this.permissions = permissions;
        this.active = active;
    }

    public static UserPrincipal create(AppUser user, List<Role> rolesWithPermissions) {
        // Extract permissions
        List<String> permissionNames = rolesWithPermissions.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(p -> p.getName())
                .distinct()
                .collect(Collectors.toList());

        List<String> roleNames = rolesWithPermissions.stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        int maxLevel = rolesWithPermissions.stream()
                .mapToInt(Role::getHierarchyLevel)
                .max()
                .orElse(0);

        // Build GrantedAuthorities: ROLE_XXX + permission names as authorities
        List<GrantedAuthority> authorities = rolesWithPermissions.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toList());

        // Add permissions as authorities too (for @PreAuthorize hasAuthority)
        authorities.addAll(permissionNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));

        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : user.getOrgId();
        String orgSlug = user.getOrganization() != null ? user.getOrganization().getSlug() : null;

        return new UserPrincipal(
                user.getId(),
                user.getUuid(),
                orgId,
                orgSlug,
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                authorities,
                maxLevel,
                roleNames,
                permissionNames,
                user.getStatus() != null && user.getStatus().name().equals("ACTIVE")
        );
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasHierarchyLevelAtLeast(int requiredLevel) {
        return this.maxHierarchyLevel >= requiredLevel;
    }

    /**
     * Hierarchical check: can this user act on target with given level?
     * e.g SUPER_ADMIN (100) can act on PROPERTY_MANAGER (80)
     */
    public boolean canActOn(int targetHierarchyLevel) {
        return this.maxHierarchyLevel > targetHierarchyLevel || this.maxHierarchyLevel == 100;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
