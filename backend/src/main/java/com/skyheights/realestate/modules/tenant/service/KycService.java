package com.skyheights.realestate.modules.tenant.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.tenant.dto.KycCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.KycResponse;
import com.skyheights.realestate.modules.tenant.entity.KycDocument;
import com.skyheights.realestate.modules.tenant.entity.TenantProfile;
import com.skyheights.realestate.modules.tenant.enums.KycDocumentType;
import com.skyheights.realestate.modules.tenant.enums.KycStatus;
import com.skyheights.realestate.modules.tenant.repository.KycDocumentRepository;
import com.skyheights.realestate.modules.tenant.repository.TenantProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycService {

    private final KycDocumentRepository kycRepository;
    private final TenantProfileRepository tenantRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final S3Service s3Service;

    @Transactional
    public KycResponse createKycDocument(Long orgId, KycCreateRequest request) {
        TenantProfile tenant = tenantRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getTenantId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        // Basic validation: Aadhaar 12 digits, PAN 10 chars etc could be added but keep lenient for now
        if (request.getDocumentNumber() != null && request.getDocumentNumber().length() < 4) {
            throw new RuntimeException("Document number too short");
        }

        KycDocument doc = KycDocument.builder()
                .organization(organizationRepository.findById(orgId).orElseThrow())
                .tenant(tenant)
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber()) // TODO: encrypt in Phase 4 with AES
                .s3Key(request.getS3Key())
                .frontS3Key(request.getFrontS3Key())
                .backS3Key(request.getBackS3Key())
                .verificationStatus(KycStatus.PENDING)
                .expiryDate(request.getExpiryDate())
                .build();

        doc = kycRepository.save(doc);
        log.info("Created KYC doc {} type {} for tenant {} org {}", doc.getId(), doc.getDocumentType(), tenant.getId(), orgId);
        return toResponse(doc);
    }

    @Transactional
    public KycResponse uploadAndCreate(Long orgId, Long tenantId, KycDocumentType type, String documentNumber,
                                       MultipartFile frontFile, MultipartFile backFile, java.time.LocalDate expiryDate) {
        TenantProfile tenant = tenantRepository.findByIdAndOrganizationIdAndIsDeletedFalse(tenantId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        String frontKey = null;
        String backKey = null;

        try {
            if (frontFile != null && !frontFile.isEmpty()) {
                String key = s3Service.generateKey(orgId, "kyc/" + tenantId, type.name() + "_front_" + frontFile.getOriginalFilename());
                frontKey = s3Service.uploadFile(key, frontFile.getInputStream(), frontFile.getSize(), frontFile.getContentType());
            }
            if (backFile != null && !backFile.isEmpty()) {
                String key = s3Service.generateKey(orgId, "kyc/" + tenantId, type.name() + "_back_" + backFile.getOriginalFilename());
                backKey = s3Service.uploadFile(key, backFile.getInputStream(), backFile.getSize(), backFile.getContentType());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload KYC files to S3: " + e.getMessage());
        }

        KycCreateRequest req = KycCreateRequest.builder()
                .tenantId(tenantId)
                .documentType(type)
                .documentNumber(documentNumber)
                .frontS3Key(frontKey)
                .backS3Key(backKey)
                .expiryDate(expiryDate)
                .build();

        return createKycDocument(orgId, req);
    }

    @Transactional(readOnly = true)
    public List<KycResponse> getKycByTenant(Long orgId, Long tenantId) {
        tenantRepository.findByIdAndOrganizationIdAndIsDeletedFalse(tenantId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return kycRepository.findByTenantIdAndIsDeletedFalse(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KycResponse getKycDocument(Long orgId, Long id) {
        KycDocument doc = kycRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));
        return toResponse(doc);
    }

    @Transactional
    public KycResponse verifyKyc(Long orgId, Long id, Long verifierUserId, boolean approved, String rejectionReason) {
        KycDocument doc = kycRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));

        AppUser verifier = appUserRepository.findByIdAndIsDeletedFalse(verifierUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Verifier user not found"));

        if (approved) {
            doc.setVerificationStatus(KycStatus.VERIFIED);
            doc.setVerifiedBy(verifier);
            doc.setVerifiedAt(Instant.now());
            doc.setRejectionReason(null);
            log.info("KYC {} verified by {} org {}", id, verifierUserId, orgId);
        } else {
            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new RuntimeException("Rejection reason required when rejecting KYC");
            }
            doc.setVerificationStatus(KycStatus.REJECTED);
            doc.setVerifiedBy(verifier);
            doc.setVerifiedAt(Instant.now());
            doc.setRejectionReason(rejectionReason);
            log.info("KYC {} rejected by {} reason {}", id, verifierUserId, rejectionReason);
        }

        doc = kycRepository.save(doc);

        // Optional: auto-update tenant status from PENDING_KYC to ACTIVE if all KYC verified
        TenantProfile tenant = doc.getTenant();
        List<KycDocument> allDocs = kycRepository.findByTenantIdAndIsDeletedFalse(tenant.getId());
        boolean allVerified = allDocs.stream().allMatch(d -> d.getVerificationStatus() == KycStatus.VERIFIED);
        if (allVerified && totalRequiredDocsMet(allDocs)) {
            if (tenant.getStatus() == com.skyheights.realestate.modules.tenant.enums.TenantStatus.PENDING_KYC) {
                tenant.setStatus(com.skyheights.realestate.modules.tenant.enums.TenantStatus.ACTIVE);
                tenantRepository.save(tenant);
                log.info("Tenant {} KYC complete, status -> ACTIVE", tenant.getId());
            }
        }

        return toResponse(doc);
    }

    @Transactional
    public void deleteKyc(Long orgId, Long id) {
        KycDocument doc = kycRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));
        doc.setIsDeleted(true);
        kycRepository.save(doc);
        log.info("Soft deleted KYC {} org {}", id, orgId);
    }

    private boolean totalRequiredDocsMet(List<KycDocument> docs) {
        // For demo, require at least AADHAAR or PASSPORT + PAN? Simplified: at least 1 doc
        return !docs.isEmpty();
    }

    private KycResponse toResponse(KycDocument doc) {
        String maskedNumber = maskDocumentNumber(doc.getDocumentNumber());
        String frontPresigned = null;
        String backPresigned = null;
        try {
            if (doc.getFrontS3Key() != null) {
                frontPresigned = s3Service.generatePresignedUrl(doc.getFrontS3Key(), Duration.ofMinutes(15));
            }
            if (doc.getBackS3Key() != null) {
                backPresigned = s3Service.generatePresignedUrl(doc.getBackS3Key(), Duration.ofMinutes(15));
            }
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL for KYC {}", doc.getId());
        }

        return KycResponse.builder()
                .id(doc.getId())
                .uuid(doc.getUuid())
                .orgId(doc.getOrganization() != null ? doc.getOrganization().getId() : null)
                .tenantId(doc.getTenant() != null ? doc.getTenant().getId() : null)
                .tenantName(doc.getTenant() != null && doc.getTenant().getUser() != null ? doc.getTenant().getUser().getFullName() : null)
                .documentType(doc.getDocumentType())
                .documentNumberMasked(maskedNumber)
                .s3Key(doc.getS3Key())
                .frontS3Key(doc.getFrontS3Key())
                .backS3Key(doc.getBackS3Key())
                .frontPresignedUrl(frontPresigned)
                .backPresignedUrl(backPresigned)
                .verificationStatus(doc.getVerificationStatus())
                .verifiedByUserId(doc.getVerifiedBy() != null ? doc.getVerifiedBy().getId() : null)
                .verifiedByName(doc.getVerifiedBy() != null ? doc.getVerifiedBy().getFullName() : null)
                .verifiedAt(doc.getVerifiedAt())
                .rejectionReason(doc.getRejectionReason())
                .expiryDate(doc.getExpiryDate())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    private String maskDocumentNumber(String number) {
        if (number == null) return null;
        if (number.length() <= 4) return "****";
        return "****" + number.substring(number.length() - 4);
    }
}
