package com.skyheights.realestate.util;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Sanitizer {

    public String sanitize(String input) {
        if (input == null) return null;
        // Basic XSS sanitization - strip script tags and encode
        String sanitized = input.replaceAll("(?i)<script.*?>.*?</script>", "")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .trim();
        if (!input.equals(sanitized)) {
            log.warn("Sanitized input from '{}' to '{}'", input, sanitized);
        }
        return sanitized;
    }

    public String sanitizePhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[^0-9]", "").trim();
    }
}
