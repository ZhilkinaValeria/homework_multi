package com.crawler.controller;

import com.crawler.model.ContactData;
import com.crawler.service.ContactCrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class CrawlerController {

    @Autowired
    private ContactCrawlerService crawlerService;

    @PostMapping("/crawl/basic")
    public ResponseEntity<Map<String, Object>> startBasicCrawling(@RequestBody List<String> urls) {
        crawlerService.startBasicCrawling(urls);

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "type", "basic",
                "message", "Запущен базовый краулер с " + urls.size() + " URL",
                "threads", "simple Thread/Runnable"
        ));
    }

    @PostMapping("/crawl/advanced")
    public ResponseEntity<Map<String, Object>> startAdvancedCrawling(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) request.get("urls");
        Integer depth = (Integer) request.getOrDefault("depth", 1);

        CompletableFuture<Map<String, Object>> future =
                crawlerService.startAdvancedCrawling(urls, depth);

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "type", "advanced",
                "message", "Запущен продвинутый краулер с глубиной " + depth,
                "urlsCount", urls.size(),
                "executor", "ExecutorService with CompletableFuture"
        ));
    }

    @GetMapping("/answer")
    public ResponseEntity<Map<String, Object>> getResults(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "date") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        List<ContactData> results = crawlerService.searchContacts(search, sort);

        // Пагинация
        int total = results.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min((page + 1) * size, total);

        List<ContactData> pageResults = results.subList(fromIndex, toIndex);

        return ResponseEntity.ok(Map.of(
                "data", pageResults,
                "pagination", Map.of(
                        "page", page,
                        "size", size,
                        "total", total,
                        "pages", (int) Math.ceil((double) total / size)
                ),
                "filters", Map.of(
                        "search", search != null ? search : "",
                        "sort", sort
                )
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(crawlerService.getStats());
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveData() {
        crawlerService.saveToFile();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Данные сохранены в файл"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> stats = crawlerService.getStats();
        return ResponseEntity.ok(Map.of(
                "status", "running",
                "service", "Multithreading Crawler",
                "timestamp", System.currentTimeMillis(),
                "stats", stats
        ));
    }
}