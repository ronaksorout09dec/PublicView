package com.skyheights.realestate.modules.tenant.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.tenant.dto.EsignCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.EsignResponse;
import com.skyheights.realestate.modules.tenant.entity.EsignTracking;
import com.skyheights.realestate.modules.tenant.entity.LeaseAgreement;
import com.skyheights.realestate.modules.tenant.enums.EsignStatus;
import com.skyheights.realestate.modules.tenant.enums.LeaseStatus;
import com.skyheights.realestate.modules.tenant.repository.EsignTrackingRepository;
import com.skyheights.realestate.modules.tenant.repository.LeaseAgreementRepository;
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
public class EsignService {

    private final EsignTrackingRepository esignRepository;
    private final LeaseAgreementRepository leaseRepository;
    private final AppUserRepository appUserRepository;
    private final S3Service s3Service;

    @Transactional
    public EsignResponse createEsignTracking(Long orgId, EsignCreateRequest request) {
        LeaseAgreement lease = leaseRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getLeaseId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        if (lease.getStatus() != LeaseStatus.DRAFT && lease.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new RuntimeException("Can only add signatories in DRAFT or PENDING_SIGNATURE status");
        }

        AppUser signer = appUserRepository.findByIdAndIsDeletedFalse(request.getSignerUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Signer user not found"));

        Long signerOrgId = signer.getOrganization() != null ? signer.getOrganization().getId() : signer.getOrgId();
        if (!signerOrgId.equals(orgId)) {
            throw new RuntimeException("Signer must belong to same org");
        }

        // Check duplicate signer role for same lease? Allow multiple but same user+role should be unique
        boolean exists = esignRepository.findByLeaseIdAndIsDeletedFalse(lease.getId()).stream()
                .anyMatch(e -> e.getSigner().getId().equals(signer.getId()) && e.getSignerRole().equalsIgnoreCase(request.getSignerRole()));
        if (exists) {
            throw new RuntimeException("Signer already added with role " + request.getSignerRole());
        }

        EsignTracking tracking = EsignTracking.builder()
                .lease(lease)
                .signer(signer)
                .signerRole(request.getSignerRole().toUpperCase())
                .status(EsignStatus.PENDING)
                .signatureOrder(request.getSignatureOrder() != null ? request.getSignatureOrder() : 1)
                .otpVerified(false)
                .expiryAt(Instant.now().plus(Duration.ofDays(7)))
                .build();

        tracking = esignRepository.save(tracking);

        // If first tracking, move lease to PENDING_SIGNATURE
        if (lease.getStatus() == LeaseStatus.DRAFT) {
            lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
            leaseRepository.save(lease);
        }

        log.info("Created esign tracking {} for lease {} signer {} org {}", tracking.getId(), lease.getId(), signer.getId(), orgId);
        return toResponse(tracking);
    }

    @Transactional(readOnly = true)
    public List<EsignResponse> getEsignTrackings(Long orgId, Long leaseId) {
        leaseRepository.findByIdAndOrganizationIdAndIsDeletedFalse(leaseId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        return esignRepository.findByLeaseIdAndIsDeletedFalse(leaseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EsignResponse sendEsign(Long orgId, Long esignId) {
        EsignTracking tracking = getTracking(orgId, esignId);

        if (tracking.getStatus() != EsignStatus.PENDING) {
            throw new RuntimeException("Only PENDING esign can be sent");
        }

        tracking.setStatus(EsignStatus.SENT);
        // In real implementation, send email/WhatsApp with OTP link
        // Mock OTP hash generation
        tracking.setOtpHash("mock-otp-hash-" + System.currentTimeMillis());
        esignRepository.save(tracking);

        log.info("Sent esign {} to {} org {} - mock OTP generated", esignId, tracking.getSigner().getEmail(), orgId);
        return toResponse(tracking);
    }

    @Transactional
    public EsignResponse markViewed(Long orgId, Long esignId) {
        EsignTracking tracking = getTracking(orgId, esignId);
        if (tracking.getStatus() == EsignStatus.SENT) {
            tracking.setStatus(EsignStatus.VIEWED);
            esignRepository.save(tracking);
        }
        return toResponse(tracking);
    }

    @Transactional
    public EsignResponse signLease(Long orgId, Long esignId, MultipartFile signatureFile, String ipAddress, String userAgent, String otp) {
        EsignTracking tracking = getTracking(orgId, esignId);

        if (tracking.getStatus() != EsignStatus.SENT && tracking.getStatus() != EsignStatus.VIEWED) {
            throw new RuntimeException("Esign must be SENT or VIEWED to sign");
        }

        if (tracking.getExpiryAt() != null && Instant.now().isAfter(tracking.getExpiryAt())) {
            tracking.setStatus(EsignStatus.EXPIRED);
            esignRepository.save(tracking);
            throw new RuntimeException("Esign link expired");
        }

        // Mock OTP verification - in prod verify against otpHash
        if (otp != null) {
            tracking.setOtpVerified(true);
        }

        // Upload signature image to S3
        if (signatureFile != null && !signatureFile.isEmpty()) {
            try {
                String key = s3Service.generateKey(orgId, "esign/" + tracking.getLease().getId(), "sig_" + tracking.getSigner().getId() + "_" + signatureFile.getOriginalFilename());
                String s3Key = s3Service.uploadFile(key, signatureFile.getInputStream(), signatureFile.getSize(), signatureFile.getContentType());
                tracking.setSignatureDataS3Key(s3Key);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload signature: " + e.getMessage());
            }
        }

        tracking.setStatus(EsignStatus.SIGNED);
        tracking.setSignedAt(Instant.now());
        tracking.setIpAddress(ipAddress);
        tracking.setUserAgent(userAgent);
        esignRepository.save(tracking);

        log.info("Esign {} signed by {} org {}", esignId, tracking.getSigner().getEmail(), orgId);

        // Check if all signed -> auto move lease to ACTIVE? Or keep manual? For now manual, but log
        LeaseAgreement lease = tracking.getLease();
        long total = esignRepository.countByLeaseIdAndIsDeletedFalse(lease.getId());
        long signed = esignRepository.countByLeaseIdAndStatusAndIsDeletedFalse(lease.getId(), EsignStatus.SIGNED);
        if (total == signed) {
            log.info("All {} signatories signed for lease {} {}, ready for activation", total, lease.getId(), lease.getLeaseNumber());
        }

        return toResponse(tracking);
    }

    @Transactional
    public EsignResponse declineEsign(Long orgId, Long esignId, String reason) {
        EsignTracking tracking = getTracking(orgId, esignId);
        tracking.setStatus(EsignStatus.DECLINED);
        esignRepository.save(tracking);
        log.info("Esign {} declined by {} reason {} org {}", esignId, tracking.getSigner().getEmail(), reason, orgId);
        return toResponse(tracking);
    }

    private EsignTracking getTracking(Long orgId, Long esignId) {
        EsignTracking tracking = esignRepository.findById(esignId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Esign tracking not found"));
        // Validate org via lease
        if (!tracking.getLease().getOrganization().getId().equals(orgId)) {
            throw new RuntimeException("Esign does not belong to your organization");
        }
        return tracking;
    }

    private EsignResponse toResponse(EsignTracking t) {
        return EsignResponse.builder()
                .id(t.getId())
                .uuid(t.getUuid())
                .leaseId(t.getLease() != null ? t.getLease().getId() : null)
                .leaseNumber(t.getLease() != null ? t.getLease().getLeaseNumber() : null)
                .signerUserId(t.getSigner() != null ? t.getSigner().getId() : null)
                .signerName(t.getSigner() != null ? t.getSigner().getFullName() : null)
                .signerEmail(t.getSigner() != null ? t.getSigner().getEmail() : null)
                .signerRole(t.getSignerRole())
                .status(t.getStatus())
                .signatureOrder(t.getSignatureOrder())
                .signatureDataS3Key(t.getSignatureDataS3Key())
                .signedAt(t.getSignedAt())
                .ipAddress(t.getIpAddress())
                .otpVerified(t.getOtpVerified())
                .expiryAt(t.getExpiryAt())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
