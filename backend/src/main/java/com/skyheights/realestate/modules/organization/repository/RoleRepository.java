package com.skyheights.realestate.modules.organization.repository;

import com.skyheights.realestate.modules.organization.entity.Role;
import com.skyheights.realestate.modules.organization.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

    Optional<Role> findByNameAndOrgId(RoleName name, Long orgId);

    Optional<Role> findByNameAndOrgIdIsNull(RoleName name); // system role

    List<Role> findByOrgIdIsNullAndIsDeletedFalse(); // system roles

    List<Role> findByOrgIdAndIsDeletedFalse(Long orgId);

    List<Role> findByOrgIdIsNullOrOrgIdAndIsDeletedFalse(Long orgId);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") Long id);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.name = :name AND r.orgId IS NULL")
    Optional<Role> findSystemRoleWithPermissions(@Param("name") RoleName name);
}
