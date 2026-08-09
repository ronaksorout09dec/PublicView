package com.skyheights.realestate.modules.organization.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.organization.dto.UserResponse;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.UserRole;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.UserRoleRepository;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('USER_MANAGE') or @permEval.hasHierarchy(80)")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersInOrg(@CurrentUser UserPrincipal currentUser,
                                                                          @RequestParam(required = false) Long orgId) {
        Long effectiveOrgId = orgId != null ? orgId : currentUser.getOrgId();
        // SUPER_ADMIN can see all orgs if orgId null and level 100
        List<AppUser> users;
        if (currentUser.getMaxHierarchyLevel() >= 100 && orgId == null) {
            users = appUserRepository.findAll().stream().filter(u -> !Boolean.TRUE.equals(u.getIsDeleted())).collect(Collectors.toList());
        } else {
            users = appUserRepository.findByOrgIdAndIsDeletedFalse(effectiveOrgId);
        }

        List<UserResponse> response = users.stream().map(user -> {
            List<UserRole> roles = userRoleRepository.findByUserIdWithRoleAndPermissions(user.getId());
            List<String> roleNames = roles.stream().map(ur -> ur.getRole().getName().name()).collect(Collectors.toList());
            List<String> perms = roles.stream().flatMap(ur -> ur.getRole().getPermissions().stream())
                    .map(p -> p.getName()).distinct().collect(Collectors.toList());
            int max = roles.stream().mapToInt(ur -> ur.getRole().getHierarchyLevel()).max().orElse(0);
            return UserResponse.builder()
                    .id(user.getId())
                    .uuid(user.getUuid())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .phone(user.getPhone())
                    .orgId(user.getOrganization() != null ? user.getOrganization().getId() : user.getOrgId())
                    .orgSlug(user.getOrganization() != null ? user.getOrganization().getSlug() : null)
                    .status(user.getStatus().name())
                    .roles(roleNames)
                    .permissions(perms)
                    .hierarchyLevel(max)
                    .createdAt(user.getCreatedAt())
                    .lastLogin(user.getLastLogin())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response, "Users fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('USER_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id, @CurrentUser UserPrincipal currentUser) {
        AppUser user = appUserRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check same org unless super admin
        if (currentUser.getMaxHierarchyLevel() < 100) {
            Long userOrgId = user.getOrganization() != null ? user.getOrganization().getId() : user.getOrgId();
            if (!userOrgId.equals(currentUser.getOrgId())) {
                throw new RuntimeException("User not in your organization");
            }
        }

        List<UserRole> roles = userRoleRepository.findByUserIdWithRoleAndPermissions(user.getId());
        List<String> roleNames = roles.stream().map(ur -> ur.getRole().getName().name()).collect(Collectors.toList());
        List<String> perms = roles.stream().flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(p -> p.getName()).distinct().collect(Collectors.toList());
        int max = roles.stream().mapToInt(ur -> ur.getRole().getHierarchyLevel()).max().orElse(0);

        UserResponse resp = UserResponse.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .orgId(user.getOrganization() != null ? user.getOrganization().getId() : user.getOrgId())
                .orgSlug(user.getOrganization() != null ? user.getOrganization().getSlug() : null)
                .status(user.getStatus().name())
                .roles(roleNames)
                .permissions(perms)
                .hierarchyLevel(max)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();

        return ResponseEntity.ok(ApiResponse.success(resp, "User fetched"));
    }
}
