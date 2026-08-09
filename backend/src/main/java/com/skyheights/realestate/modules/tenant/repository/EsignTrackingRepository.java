package com.skyheights.realestate.modules.tenant.repository;

import com.skyheights.realestate.modules.tenant.entity.EsignTracking;
import com.skyheights.realestate.modules.tenant.enums.EsignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsignTrackingRepository extends JpaRepository<EsignTracking, Long> {

    List<EsignTracking> findByLeaseIdAndIsDeletedFalse(Long leaseId);

    List<EsignTracking> findByLeaseIdAndStatusAndIsDeletedFalse(Long leaseId, EsignStatus status);

    List<EsignTracking> findBySignerIdAndIsDeletedFalse(Long signerId);

    long countByLeaseIdAndStatusAndIsDeletedFalse(Long leaseId, EsignStatus status);

    long countByLeaseIdAndIsDeletedFalse(Long leaseId);
}
