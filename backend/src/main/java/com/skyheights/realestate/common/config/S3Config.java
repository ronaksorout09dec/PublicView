package com.skyheights.realestate.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 Config - Prop-OS Document Layer
 * Stores KYC, Lease PDFs, Ticket Media, Condition Photos, Invoices, Receipts
 * For Phase 2, it's a placeholder - actual upload service comes in Phase 3+
 * Falls back to mock if credentials missing
 */
@Configuration
@Slf4j
public class S3Config {

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.bucket:propos-documents-dev}")
    private String bucket;

    @Value("${aws.accessKeyId:}")
    private String accessKeyId;

    @Value("${aws.secretAccessKey:}")
    private String secretAccessKey;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    @Bean
    public S3Client s3Client() {
        try {
            if (!s3Enabled || accessKeyId.isBlank() || secretAccessKey.isBlank()) {
                log.warn("S3 disabled or credentials missing - S3Client created with dummy creds (NOOP). Set aws.s3.enabled=true for prod");
            } else {
                log.info("Configuring S3Client region {} bucket {}", region, bucket);
            }
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    accessKeyId.isBlank() ? "dummy" : accessKeyId,
                                    secretAccessKey.isBlank() ? "dummy" : secretAccessKey
                            )
                    ))
                    .build();
        } catch (Exception e) {
            log.error("Failed to create S3Client, creating dummy for fallback", e);
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("dummy","dummy")
                    ))
                    .build();
        }
    }

    @Bean
    public S3Presigner s3Presigner() {
        try {
            if (!s3Enabled || accessKeyId.isBlank()) {
                log.warn("S3Presigner created with dummy creds - presigned URLs will be mocked until enabled");
            }
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    accessKeyId.isBlank() ? "dummy" : accessKeyId,
                                    secretAccessKey.isBlank() ? "dummy" : secretAccessKey
                            )
                    ))
                    .build();
        } catch (Exception e) {
            log.error("Failed to create S3Presigner, creating dummy", e);
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("dummy","dummy")
                    ))
                    .build();
        }
    }
}
