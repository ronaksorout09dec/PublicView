package com.skyheights.realestate.modules.organization.service;

import com.skyheights.realestate.modules.organization.dto.JwtResponse;
import com.skyheights.realestate.modules.organization.dto.LoginRequest;
import com.skyheights.realestate.modules.organization.dto.RegisterRequest;
import com.skyheights.realestate.modules.organization.dto.UserResponse;
import com.skyheights.realestate.modules.organization.entity.*;
import com.skyheights.realestate.modules.organization.enums.RoleName;
import com.skyheights.realestate.modules.organization.repository.*;
import com.skyheights.realestate.security.JwtTokenProvider;
import com.skyheights.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enterprise Auth Service - Prop-OS
 * Handles login, register org + user, refresh token
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String jwt = tokenProvider.generateToken(authentication);
        String refresh = tokenProvider.generateRefreshToken(authentication);

        // Update last login
        appUserRepository.findById(userPrincipal.getId()).ifPresent(u -> {
            u.setLastLogin(Instant.now());
            appUserRepository.save(u);
        });

        Organization org = null;
        if (userPrincipal.getOrgId() != null) {
            org = organizationRepository.findById(userPrincipal.getOrgId()).orElse(null);
        }

        log.info("User {} logged in, orgId {}", userPrincipal.getUsername(), userPrincipal.getOrgId());

        return JwtResponse.builder()
                .accessToken(jwt)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .userId(userPrincipal.getId())
                .uuid(userPrincipal.getUuid())
                .email(userPrincipal.getUsername())
                .fullName(userPrincipal.getFullName())
                .orgId(userPrincipal.getOrgId())
                .orgSlug(userPrincipal.getOrgSlug())
                .orgName(org != null ? org.getName() : null)
                .roles(userPrincipal.getRoles())
                .permissions(userPrincipal.getPermissions())
                .hierarchyLevel(userPrincipal.getMaxHierarchyLevel())
                .build();
    }

    @Transactional
    public JwtResponse register(RegisterRequest request) {
        // Check if email already exists in org scope
        if (request.getOrgSlug() != null) {
            organizationRepository.findBySlug(request.getOrgSlug()).ifPresent(org -> {
                if (appUserRepository.existsByEmailIgnoreCaseAndOrgId(request.getEmail(), org.getId())) {
                    throw new RuntimeException("Email already exists in organization " + request.getOrgSlug());
                }
            });
        }

        // Org creation flow
        Organization organization;
        if (request.getOrgSlug() != null) {
            organization = organizationRepository.findBySlugAndIsDeletedFalse(request.getOrgSlug())
                    .orElseThrow(() -> new RuntimeException("Organization not found: " + request.getOrgSlug()));
        } else {
            // Create new org for PROPERTY_MANAGER
            if (request.getOrgName() == null) {
                request.setOrgName(request.getFullName() + "'s Organization");
            }
            String slug = request.getOrgSlug() != null ? request.getOrgSlug()
                    : request.getOrgName().toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + System.currentTimeMillis() % 10000;

            if (organizationRepository.existsBySlug(slug)) {
                slug = slug + "-" + System.currentTimeMillis() % 1000;
            }

            organization = Organization.builder()
                    .name(request.getOrgName())
                    .slug(slug)
                    .billingEmail(request.getEmail())
                    .build();
            organization = organizationRepository.save(organization);
            log.info("Created new organization {} slug {}", organization.getName(), slug);
        }

        // Create user
        AppUser user = AppUser.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .organization(organization)
                .build();
        user = appUserRepository.save(user);

        // Assign role
        RoleName roleName = request.getRole() != null ? request.getRole() : RoleName.PROPERTY_MANAGER;
        Role role = roleRepository.findByNameAndOrgIdIsNull(roleName)
                .or(() -> roleRepository.findByName(roleName))
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .organization(organization)
                .build();
        userRoleRepository.save(userRole);

        // If org owner not set, set first PROPERTY_MANAGER as owner
        if (organization.getOwnerUserId() == null && roleName == RoleName.PROPERTY_MANAGER) {
            organization.setOwnerUserId(user.getId());
            organizationRepository.save(organization);
        }

        // Auto login after register
        LoginRequest loginReq = new LoginRequest(request.getEmail(), request.getPassword(), organization.getSlug());
        return login(loginReq);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        AppUser user = appUserRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserRole> userRoles = userRoleRepository.findByUserIdWithRoleAndPermissions(userId);
        List<String> roles = userRoles.stream().map(ur -> ur.getRole().getName().name()).collect(Collectors.toList());
        List<String> perms = userRoles.stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(p -> p.getName()).distinct().collect(Collectors.toList());
        int maxLevel = userRoles.stream().mapToInt(ur -> ur.getRole().getHierarchyLevel()).max().orElse(0);

        Organization org = user.getOrganization();
        if (org == null && user.getOrgId() != null) {
            org = organizationRepository.findById(user.getOrgId()).orElse(null);
        }

        return UserResponse.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .orgId(org != null ? org.getId() : user.getOrgId())
                .orgSlug(org != null ? org.getSlug() : null)
                .status(user.getStatus().name())
                .roles(roles)
                .permissions(perms)
                .hierarchyLevel(maxLevel)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    /**
     * Hierarchical permission check - can user with hierarchyLevel act on target level?
     */
    public boolean canActOn(Long actorUserId, int targetHierarchyLevel) {
        AppUser actor = appUserRepository.findById(actorUserId).orElseThrow();
        List<UserRole> roles = userRoleRepository.findByUserIdWithRoleAndPermissions(actorUserId);
        int max = roles.stream().mapToInt(r -> r.getRole().getHierarchyLevel()).max().orElse(0);
        return max > targetHierarchyLevel || max == 100; // 100 SUPER_ADMIN can act on all
    }
}
