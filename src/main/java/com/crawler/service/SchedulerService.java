package com.crawler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private H2StorageService storageService;

    private final Set<String> defaultStartUrls = ConcurrentHashMap.newKeySet();

    public SchedulerService() {
        // Стартовые URLs для реальных сайтов
        // 2GIS
        defaultStartUrls.add("https://2gis.ru");
        defaultStartUrls.add("https://2gis.ru/moscow");
        defaultStartUrls.add("https://2gis.ru/spb");

        // Яндекс.Справочник
        defaultStartUrls.add("https://yandex.ru/maps");
        defaultStartUrls.add("https://yandex.ru/maps/213/moscow");
        defaultStartUrls.add("https://yandex.ru/maps/2/saint-petersburg");

        // Популярные корпоративные сайты
        defaultStartUrls.add("https://www.sberbank.ru");
        defaultStartUrls.add("https://www.gazprom.ru");
        defaultStartUrls.add("https://www.lukoil.ru");
        defaultStartUrls.add("https://www.rosneft.ru");
        defaultStartUrls.add("https://www.tatneft.ru");
        defaultStartUrls.add("https://www.megafon.ru");
        defaultStartUrls.add("https://www.mts.ru");
        defaultStartUrls.add("https://www.beeline.ru");
        defaultStartUrls.add("https://www.tele2.ru");
        defaultStartUrls.add("https://www.yandex.ru");
        defaultStartUrls.add("https://www.mail.ru");
        defaultStartUrls.add("https://www.vk.com");
        defaultStartUrls.add("https://www.avito.ru");
        defaultStartUrls.add("https://www.wildberries.ru");
        defaultStartUrls.add("https://www.ozon.ru");
        defaultStartUrls.add("https://www.dns-shop.ru");
        defaultStartUrls.add("https://www.citilink.ru");
        defaultStartUrls.add("https://www.eldorado.ru");
        defaultStartUrls.add("https://www.mvideo.ru");
    }

    @Scheduled(fixedRate = 3600000) // Запуск каждый час
    public void scheduledCrawling() {
        logger.info("Scheduled crawling started at: {}", new java.util.Date());

        try {
            crawlerService.startCrawling(defaultStartUrls, 2, 50);

            logger.info("Scheduled crawling completed. Total records: {}",
                    storageService.getDataCount());

        } catch (Exception e) {
            logger.error("Error in scheduled crawling: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 300000) // Каждые 5 минут
    public void statusReport() {
        int dataCount = storageService.getDataCount();
        int visitedUrls = crawlerService.getVisitedUrls().size();
        int activeTasks = crawlerService.getActiveTasks();

        logger.info("Status Report - Data records: {}, Visited URLs: {}, Active tasks: {}",
                dataCount, visitedUrls, activeTasks);
    }

    public void addStartUrl(String url) {
        defaultStartUrls.add(url);
        logger.info("Added URL to scheduler: {}", url);
    }

    public Set<String> getStartUrls() {
        return Set.copyOf(defaultStartUrls);
    }
}