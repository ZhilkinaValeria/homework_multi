package com.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebCrawlerApplication.class, args);
        System.out.println("=== Web Crawler Application Started ===");
        System.out.println("Available endpoints:");
        System.out.println("  GET  http://localhost:8080/api/data/answer");
        System.out.println("  POST http://localhost:8080/api/crawler/start");
        System.out.println("  GET  http://localhost:8080/api/data/contacts/sorted?field=phones&ascending=false");
        System.out.println("  GET  http://localhost:8080/api/data/contacts/filter?search=+7999");
        System.out.println("========================================");
    }
}
