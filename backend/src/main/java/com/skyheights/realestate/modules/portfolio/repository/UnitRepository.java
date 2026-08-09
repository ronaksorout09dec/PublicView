package com.skyheights.realestate.modules.portfolio.repository;

import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.portfolio.enums.UnitStatus;
import com.skyheights.realestate.modules.portfolio.enums.UnitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<Unit> findByPropertyIdAndIsDeletedFalse(Long propertyId);

    Page<Unit> findByPropertyIdAndIsDeletedFalse(Long propertyId, Pageable pageable);

    Page<Unit> findByOrganizationIdAndPropertyIdAndIsDeletedFalse(Long orgId, Long propertyId, Pageable pageable);

    Page<Unit> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    Page<Unit> findByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, UnitStatus status, Pageable pageable);

    boolean existsByPropertyIdAndUnitNumberAndIsDeletedFalse(Long propertyId, String unitNumber);

    boolean existsByPropertyIdAndUnitNumberAndIdNotAndIsDeletedFalse(Long propertyId, String unitNumber, Long id);

    long countByPropertyIdAndIsDeletedFalse(Long propertyId);

    long countByPropertyIdAndStatusAndIsDeletedFalse(Long propertyId, UnitStatus status);

    long countByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, UnitStatus status);

    @Query("SELECT u FROM Unit u WHERE u.organization.id = :orgId AND u.isDeleted = false " +
            "AND (:propertyId IS NULL OR u.property.id = :propertyId) " +
            "AND (:status IS NULL OR u.status = :status) " +
            "AND (:type IS NULL OR u.type = :type) " +
            "AND (:search IS NULL OR LOWER(u.unitNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Unit> search(@Param("orgId") Long orgId,
                      @Param("propertyId") Long propertyId,
                      @Param("status") UnitStatus status,
                      @Param("type") UnitType type,
                      @Param("search") String search,
                      Pageable pageable);

    @Query("SELECT u FROM Unit u LEFT JOIN FETCH u.amenities WHERE u.id = :id AND u.organization.id = :orgId AND u.isDeleted = false")
    Optional<Unit> findByIdWithAmenities(@Param("id") Long id, @Param("orgId") Long orgId);

    List<Unit> findByOrganizationIdAndStatusAndPropertyIdAndIsDeletedFalse(Long orgId, UnitStatus status, Long propertyId);
}
