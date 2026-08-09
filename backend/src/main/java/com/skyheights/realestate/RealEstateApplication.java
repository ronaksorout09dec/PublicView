package com.skyheights.realestate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.skyheights.realestate.config.OllamaConfig;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableConfigurationProperties(OllamaConfig.class)
@EnableCaching
@EnableJpaAuditing
@EnableScheduling
@Slf4j
public class RealEstateApplication {

    public static void main(String[] args) {
        log.info("==================================================");
        log.info(" Starting Prop-OS — Ultimate Property Operating System");
        log.info(" Phase 2: Security + RBAC + JWT + Redis + S3");
        log.info("==================================================");
        log.info("Features: Portfolio, CRM, Financial, Tenant Legal, Maintenance Bidding, Communication, IoT");
        log.info("Security: JWT (access 15m, refresh 7d), Hierarchical RBAC, BCrypt(12)");
        log.info("Cache: Redis (properties, units, tenants, invoices, accessPins)");
        log.info("Storage: AWS S3 (KYC, Lease PDFs, Ticket Media, Invoices)");
        log.info("Legacy: Ollama Qwen3 AI Voice Agent still supported at /api/voice/*");
        log.info("==================================================");
        SpringApplication.run(RealEstateApplication.class, args);
        log.info("Prop-OS started successfully — Ready to dominate Real Estate");
        log.info("APIs: POST /api/auth/register, POST /api/auth/login, GET /api/auth/me, GET /api/health");
        log.info("Default creds: superadmin@propos.io / SuperAdmin123!  |  manager@demo.com / Manager123!");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.info("Shutting down Prop-OS")));
    }
}
