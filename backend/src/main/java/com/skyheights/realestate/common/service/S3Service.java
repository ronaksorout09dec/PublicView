package com.skyheights.realestate.common.service;

import com.skyheights.realestate.common.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * Prop-OS S3 Service - Document Storage Abstraction
 * Phase 2: Placeholder with presigned URL generation
 * Phase 3+: Full upload/download, multipart, lifecycle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket:propos-documents-dev}")
    private String bucket;

    @Value("${aws.s3.enabled:false}")
    private boolean enabled;

    /**
     * Generates S3 key with org isolation: {org_id}/{entity}/{year}/{uuid}-{filename}
     */
    public String generateKey(Long orgId, String entity, String filename) {
        int year = java.time.Year.now().getValue();
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("%d/%s/%d/%s-%s", orgId, entity, year, UUID.randomUUID(), sanitized);
    }

    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        if (!enabled || s3Client == null) {
            log.warn("S3 upload mocked - S3 disabled. Key: {}, contentType: {}", key, contentType);
            return key; // Mock: return key without actual upload
        }
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            log.info("Uploaded file to S3 bucket {} key {}", bucket, key);
            return key;
        } catch (Exception e) {
            log.error("Failed to upload to S3 key {}", key, e);
            throw new RuntimeException("S3 upload failed: " + e.getMessage());
        }
    }

    public String generatePresignedUrl(String key, Duration expiration) {
        if (!enabled || s3Presigner == null) {
            log.warn("S3 presigned URL mocked for key {}", key);
            return "https://mock-s3.local/" + bucket + "/" + key + "?expires=" + expiration.toMinutes() + "m";
        }
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(b -> b.bucket(bucket).key(key))
                    .build();
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key {}", key, e);
            throw new RuntimeException("Presigned URL failed");
        }
    }

    public String getBucket() {
        return bucket;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
