package com.skyheights.realestate.modules.maintenance.repository;

import com.skyheights.realestate.modules.maintenance.entity.VendorProfile;
import com.skyheights.realestate.modules.maintenance.enums.VendorSpecialization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorProfileRepository extends JpaRepository<VendorProfile, Long> {

    Optional<VendorProfile> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Optional<VendorProfile> findByUserIdAndIsDeletedFalse(Long userId);

    Page<VendorProfile> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<VendorProfile> findByOrganizationIdAndIsDeletedFalse(Long orgId);

    @Query("SELECT v FROM VendorProfile v WHERE v.organization.id = :orgId AND v.isDeleted = false " +
            "AND (:specialization IS NULL OR v.specialization = :specialization) " +
            "AND (:search IS NULL OR LOWER(v.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:isVerified IS NULL OR v.isVerified = :isVerified)")
    Page<VendorProfile> search(@Param("orgId") Long orgId,
                               @Param("specialization") VendorSpecialization specialization,
                               @Param("search") String search,
                               @Param("isVerified") Boolean isVerified,
                               Pageable pageable);

    List<VendorProfile> findByOrganizationIdAndSpecializationAndIsDeletedFalse(Long orgId, VendorSpecialization specialization);
}
