package com.skyheights.realestate.modules.organization.repository;

import com.skyheights.realestate.modules.organization.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByUserIdAndOrgId(Long userId, Long orgId);

    void deleteByUserIdAndRoleIdAndOrgId(Long userId, Long roleId, Long orgId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role r LEFT JOIN FETCH r.permissions WHERE ur.user.id = :userId")
    List<UserRole> findByUserIdWithRoleAndPermissions(@Param("userId") Long userId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role r WHERE ur.user.id = :userId AND ur.organization.id = :orgId")
    List<UserRole> findByUserIdAndOrgIdWithRole(@Param("userId") Long userId, @Param("orgId") Long orgId);
}
