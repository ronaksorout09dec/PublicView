package com.skyheights.realestate.modules.tenant.repository;

import com.skyheights.realestate.modules.tenant.entity.TenantProfile;
import com.skyheights.realestate.modules.tenant.enums.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantProfileRepository extends JpaRepository<TenantProfile, Long> {

    Optional<TenantProfile> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Page<TenantProfile> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<TenantProfile> findByUnitIdAndIsDeletedFalse(Long unitId);

    Optional<TenantProfile> findByUserIdAndIsDeletedFalse(Long userId);

    boolean existsByUnitIdAndStatusAndIsDeletedFalse(Long unitId, TenantStatus status);

    @Query("SELECT t FROM TenantProfile t WHERE t.organization.id = :orgId AND t.isDeleted = false " +
            "AND (:propertyId IS NULL OR t.property.id = :propertyId) " +
            "AND (:unitId IS NULL OR t.unit.id = :unitId) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:search IS NULL OR LOWER(t.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR t.user.email LIKE CONCAT('%', :search, '%') OR t.user.phone LIKE CONCAT('%', :search, '%'))")
    Page<TenantProfile> search(@Param("orgId") Long orgId,
                               @Param("propertyId") Long propertyId,
                               @Param("unitId") Long unitId,
                               @Param("status") TenantStatus status,
                               @Param("search") String search,
                               Pageable pageable);

    long countByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, TenantStatus status);
}
