package com.crawler.scheduler;

import com.crawler.service.ContactCrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class LoggingDaemon {

    private final ScheduledExecutorService daemonScheduler;
    private final ContactCrawlerService crawlerService;

    @Autowired
    public LoggingDaemon(ContactCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
        this.daemonScheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "logging-daemon");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void start() {
        // Демон-поток для подробного логирования каждые 30 секунд
        daemonScheduler.scheduleAtFixedRate(() -> {
            try {
                Runtime runtime = Runtime.getRuntime();
                long memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
                long memoryMax = runtime.maxMemory() / (1024 * 1024);

                System.out.println("=== ДЕМОН-МОНИТОРИНГ ===");
                System.out.println("Память: " + memoryUsed + "MB / " + memoryMax + "MB");
                System.out.println("Процессоры: " + runtime.availableProcessors());
                System.out.println("========================");
            } catch (Exception e) {
                System.err.println("Ошибка в демон-потоке: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);

        System.out.println("Демон-поток логирования запущен");
    }

    @PreDestroy
    public void shutdown() {
        daemonScheduler.shutdown();
        try {
            if (!daemonScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                daemonScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            daemonScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Демон-поток логирования остановлен");
    }
}
