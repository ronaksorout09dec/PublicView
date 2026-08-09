package com.skyheights.realestate.modules.maintenance.repository;

import com.skyheights.realestate.modules.maintenance.entity.VendorPayout;
import com.skyheights.realestate.modules.maintenance.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorPayoutRepository extends JpaRepository<VendorPayout, Long> {

    Optional<VendorPayout> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    Page<VendorPayout> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    List<VendorPayout> findByVendorIdAndIsDeletedFalse(Long vendorId);

    Page<VendorPayout> findByVendorIdAndIsDeletedFalse(Long vendorId, Pageable pageable);

    @Query("SELECT p FROM VendorPayout p WHERE p.organization.id = :orgId AND p.isDeleted = false " +
            "AND (:vendorId IS NULL OR p.vendor.id = :vendorId) " +
            "AND (:status IS NULL OR p.status = :status)")
    Page<VendorPayout> search(@Param("orgId") Long orgId,
                              @Param("vendorId") Long vendorId,
                              @Param("status") PayoutStatus status,
                              Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.netPayable),0) FROM VendorPayout p WHERE p.vendor.id = :vendorId AND p.status = :status AND p.isDeleted = false")
    java.math.BigDecimal sumNetPayableByVendorAndStatus(@Param("vendorId") Long vendorId, @Param("status") PayoutStatus status);
}
