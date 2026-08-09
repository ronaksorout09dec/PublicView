package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.UtilityMeter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilityMeterRepository extends JpaRepository<UtilityMeter, Long> {

    Optional<UtilityMeter> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<UtilityMeter> findByOrganizationIdAndIsDeletedFalse(Long orgId);

    List<UtilityMeter> findByPropertyIdAndIsDeletedFalse(Long propertyId);

    List<UtilityMeter> findByUnitIdAndIsDeletedFalse(Long unitId);

    List<UtilityMeter> findByPropertyIdAndUtilityTypeIdAndIsDeletedFalse(Long propertyId, Long utilityTypeId);

    Optional<UtilityMeter> findByMeterNumber(String meterNumber);
}
