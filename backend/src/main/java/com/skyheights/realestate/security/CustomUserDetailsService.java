package com.skyheights.realestate.security;

import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.entity.Role;
import com.skyheights.realestate.modules.organization.entity.UserRole;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads user by email (format: email|orgSlug or email alone for SUPER_ADMIN)
 * For multi-tenancy, we support email alone + resolve org via context, but Phase 2 loads by email (first match)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username can be email or email:orgId? For simplicity, email
        // If multiple orgs same email? We pick first active.
        String email = username.contains(":") ? username.split(":")[0] : username;

        AppUser user = appUserRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new UsernameNotFoundException("User deleted: " + email);
        }

        List<UserRole> userRoles = userRoleRepository.findByUserIdWithRoleAndPermissions(user.getId());
        List<Role> roles = userRoles.stream().map(UserRole::getRole).collect(Collectors.toList());

        log.debug("Loaded user {} with {} roles", email, roles.size());

        return UserPrincipal.create(user, roles);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        List<UserRole> userRoles = userRoleRepository.findByUserIdWithRoleAndPermissions(user.getId());
        List<Role> roles = userRoles.stream().map(UserRole::getRole).collect(Collectors.toList());

        return UserPrincipal.create(user, roles);
    }
}
