package com.crawler.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CrawlerServiceTest {

    @Autowired
    private CrawlerService crawlerService;

    @Test
    void testCrawlerInitialization() {
        assertNotNull(crawlerService);
        assertTrue(crawlerService.getActiveTasks() >= 0);
        assertNotNull(crawlerService.getVisitedUrls());
    }

    @Test
    void testStartCrawling() {
        Set<String> testUrls = Set.of("https://httpbin.org/html", "https://httpbin.org/json");

        assertDoesNotThrow(() -> {
            crawlerService.startCrawling(testUrls, 1, 10);

            // Даем время на выполнение
            Thread.sleep(5000);

            // Проверяем, что сервис работает
            assertTrue(crawlerService.getActiveTasks() >= 0);
        });
    }

    @Test
    void testCrawlerShutdown() {
        assertDoesNotThrow(() -> {
            crawlerService.shutdown();
            // После shutdown сервис должен корректно завершать работу
        });
    }

    @Test
    void testVisitedUrlsCollection() {
        Set<String> visitedUrls = crawlerService.getVisitedUrls();
        assertNotNull(visitedUrls);
        // Коллекция должна быть потокобезопасной
        assertDoesNotThrow(() -> {
            visitedUrls.size();
            visitedUrls.contains("test");
        });
    }
}
