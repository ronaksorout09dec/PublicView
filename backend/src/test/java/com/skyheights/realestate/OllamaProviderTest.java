package com.skyheights.realestate;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.skyheights.realestate.ai.OllamaProvider;
import com.skyheights.realestate.service.PromptService;

@SpringBootTest
class OllamaProviderTest {

    @Autowired(required = false)
    private OllamaProvider ollamaProvider;

    @Autowired
    private PromptService promptService;

    @Test
    void testPromptGeneration() {
        String prompt = promptService.generateSystemPrompt();
        assertNotNull(prompt);
        assertTrue(prompt.contains("Sky Heights Residency"));
        assertTrue(prompt.contains("Sector 150"));
    }

    @Test
    void testProjectInfoCached() {
        String info = promptService.getProjectInfo();
        assertNotNull(info);
        assertTrue(info.contains("85 Lakhs"));
    }
}
