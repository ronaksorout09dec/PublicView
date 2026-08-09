package com.skyheights.realestate.modules.portfolio.repository;

import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.enums.PropertyStatus;
import com.skyheights.realestate.modules.portfolio.enums.PropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    Optional<Property> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<Property> findByOrganizationIdAndIsDeletedFalse(Long orgId);

    Page<Property> findByOrganizationIdAndIsDeletedFalse(Long orgId, Pageable pageable);

    Page<Property> findByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, PropertyStatus status, Pageable pageable);

    Page<Property> findByOrganizationIdAndTypeAndIsDeletedFalse(Long orgId, PropertyType type, Pageable pageable);

    boolean existsByOrganizationIdAndNameAndIsDeletedFalse(Long orgId, String name);

    @Query("SELECT p FROM Property p WHERE p.organization.id = :orgId AND p.isDeleted = false " +
            "AND (:city IS NULL OR LOWER(p.city) = LOWER(:city)) " +
            "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.address) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:type IS NULL OR p.type = :type) " +
            "AND (:status IS NULL OR p.status = :status)")
    Page<Property> search(@Param("orgId") Long orgId,
                          @Param("city") String city,
                          @Param("search") String search,
                          @Param("type") PropertyType type,
                          @Param("status") PropertyStatus status,
                          Pageable pageable);

    @Query("SELECT p FROM Property p LEFT JOIN FETCH p.amenities WHERE p.id = :id AND p.organization.id = :orgId AND p.isDeleted = false")
    Optional<Property> findByIdWithAmenities(@Param("id") Long id, @Param("orgId") Long orgId);

    long countByOrganizationIdAndIsDeletedFalse(Long orgId);

    long countByOrganizationIdAndStatusAndIsDeletedFalse(Long orgId, PropertyStatus status);
}
