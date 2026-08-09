package com.skyheights.realestate.modules.tenant.repository;

import com.skyheights.realestate.modules.tenant.entity.KycDocument;
import com.skyheights.realestate.modules.tenant.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    Optional<KycDocument> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<KycDocument> findByTenantIdAndIsDeletedFalse(Long tenantId);

    List<KycDocument> findByTenantIdAndVerificationStatusAndIsDeletedFalse(Long tenantId, KycStatus status);

    List<KycDocument> findByOrganizationIdAndVerificationStatusAndIsDeletedFalse(Long orgId, KycStatus status);

    boolean existsByTenantIdAndDocumentTypeAndIsDeletedFalse(Long tenantId, com.skyheights.realestate.modules.tenant.enums.KycDocumentType type);
}
