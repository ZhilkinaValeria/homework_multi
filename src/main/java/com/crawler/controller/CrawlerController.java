package com.crawler.controller;

import com.crawler.service.CrawlerService;
import com.crawler.service.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private SchedulerService schedulerService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startCrawling(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Set<String> urls = (Set<String>) request.get("urls");
        int maxDepth = (int) request.getOrDefault("maxDepth", 2);
        int maxPages = (int) request.getOrDefault("maxPages", 100);

        crawlerService.startCrawling(urls, maxDepth, maxPages);

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "urls", String.valueOf(urls.size()),
                "maxDepth", String.valueOf(maxDepth),
                "maxPages", String.valueOf(maxPages)
        ));
    }

    @PostMapping("/add-url")
    public ResponseEntity<Map<String, String>> addStartUrl(@RequestParam String url) {
        schedulerService.addStartUrl(url);
        return ResponseEntity.ok(Map.of("status", "added", "url", url));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "startUrls", schedulerService.getStartUrls().size(),
                "visitedUrls", crawlerService.getVisitedUrls().size(),
                "activeTasks", crawlerService.getActiveTasks()
        ));
    }
}
