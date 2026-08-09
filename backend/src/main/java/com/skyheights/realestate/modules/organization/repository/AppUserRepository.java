package com.skyheights.realestate.modules.organization.repository;

import com.skyheights.realestate.modules.organization.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<AppUser> findByEmailIgnoreCaseAndIsDeletedFalse(String email);

    Optional<AppUser> findByIdAndIsDeletedFalse(Long id);

    List<AppUser> findByOrgIdAndIsDeletedFalse(Long orgId);

    boolean existsByEmailIgnoreCaseAndOrgId(String email, Long orgId);

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.organization WHERE u.email = :email AND u.isDeleted = false")
    List<AppUser> findByEmailWithOrg(@Param("email") String email);

    @Query("SELECT u FROM AppUser u WHERE u.orgId = :orgId AND u.isDeleted = false")
    List<AppUser> findActiveByOrgId(@Param("orgId") Long orgId);

    Optional<AppUser> findByPhoneAndIsDeletedFalse(String phone);
}
