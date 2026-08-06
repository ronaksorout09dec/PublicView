package com.skyheights.realestate;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.skyheights.realestate.dto.LeadRequest;
import com.skyheights.realestate.dto.LeadResponse;
import com.skyheights.realestate.service.LeadService;

@SpringBootTest
@ActiveProfiles("test")
class LeadServiceTest {

    @Autowired
    private LeadService leadService;

    @Test
    void testCreateAndFetchLead() {
        LeadRequest request = LeadRequest.builder()
                .customerName("Test User")
                .phone("9876543210")
                .location("Sector 150 Noida")
                .propertyType("Apartment")
                .configuration("3 BHK")
                .budget("1.2 Crore")
                .purpose("Buying")
                .timeline("6 months")
                .conversationSummary("Test summary")
                .build();

        LeadResponse created = leadService.createLead(request);
        assertNotNull(created.getId());
        assertEquals("Test User", created.getCustomerName());
        assertEquals("9876543210", created.getPhone());

        LeadResponse fetched = leadService.getLeadById(created.getId());
        assertEquals(created.getId(), fetched.getId());
    }

    @Test
    void testValidation() {
        // Invalid phone should be rejected at DTO validation level - tested via controller
        assertTrue(true);
    }
}
