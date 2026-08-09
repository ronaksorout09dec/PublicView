package com.skyheights.realestate.modules.organization.repository;

import com.skyheights.realestate.modules.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findBySlug(String slug);

    Optional<Organization> findBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlug(String slug);

    Optional<Organization> findByIdAndIsDeletedFalse(Long id);
}
