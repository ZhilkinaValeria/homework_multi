package com.crawler.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n=========================================");
        System.out.println("   МНОГОПОТОЧНЫЙ КРАУЛЕР АКТИВИРОВАН");
        System.out.println("=========================================");
        System.out.println("Доступные эндпоинты:");
        System.out.println("POST /api/crawl/basic    - Базовый краулер (Thread/Runnable)");
        System.out.println("POST /api/crawl/advanced - Продвинутый краулер (ExecutorService)");
        System.out.println("GET  /api/answer         - Результаты с пагинацией");
        System.out.println("GET  /api/stats          - Статистика");
        System.out.println("POST /api/save           - Сохранение в файл");
        System.out.println("GET  /api/health         - Статус сервиса");
        System.out.println("=========================================");
        System.out.println("Пример запроса для запуска:");
        System.out.println("POST http://localhost:8080/api/crawl/basic");
        System.out.println("Content-Type: application/json");
        System.out.println("Body: [\"https://example.com\"]");
        System.out.println("=========================================\n");
    }
}