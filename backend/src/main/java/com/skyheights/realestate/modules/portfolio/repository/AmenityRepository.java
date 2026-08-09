package com.skyheights.realestate.modules.portfolio.repository;

import com.skyheights.realestate.modules.portfolio.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    Optional<Amenity> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<Amenity> findByOrganizationIdAndIsDeletedFalse(Long orgId);

    boolean existsByOrganizationIdAndNameAndIsDeletedFalse(Long orgId, String name);

    boolean existsByOrganizationIdAndNameAndIdNotAndIsDeletedFalse(Long orgId, String name, Long id);
}
