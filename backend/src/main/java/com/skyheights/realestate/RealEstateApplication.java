package com.skyheights.realestate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.skyheights.realestate.config.OllamaConfig;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableConfigurationProperties(OllamaConfig.class)
@Slf4j
public class RealEstateApplication {

    public static void main(String[] args) {
        log.info("Starting Sky Heights Residency - AI Real Estate Voice Agent");
        SpringApplication.run(RealEstateApplication.class, args);
        log.info("Application started successfully. Sky Heights Residency is ready to serve.");
    }
}
