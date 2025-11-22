package com.crawler.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SchedulerServiceTest {

    @Autowired
    private SchedulerService schedulerService;

    @Test
    void testSchedulerInitialization() {
        assertNotNull(schedulerService);

        Set<String> startUrls = schedulerService.getStartUrls();
        assertNotNull(startUrls);
        assertFalse(startUrls.isEmpty());
    }

    @Test
    void testAddStartUrl() {
        String testUrl = "https://test-add-url.com";

        schedulerService.addStartUrl(testUrl);

        Set<String> urls = schedulerService.getStartUrls();
        assertTrue(urls.contains(testUrl));
    }

    @Test
    void testScheduledMethods() {
        // Проверяем, что методы расписания не выбрасывают исключения
        assertDoesNotThrow(() -> {
            schedulerService.scheduledCrawling();
        });

        assertDoesNotThrow(() -> {
            schedulerService.statusReport();
        });
    }
}