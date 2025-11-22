package com.crawler.service;

import com.crawler.model.ContactInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CrawlerService {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);

    @Autowired
    private ContactExtractorService contactExtractorService;

    @Autowired
    private H2StorageService storageService;

    private final RestTemplate restTemplate;
    private final Set<String> visitedUrls;
    private final Set<String> processingUrls;
    private final AtomicInteger activeTasks;
    private final ReentrantLock lock;

    private ForkJoinPool crawlerForkJoinPool;
    private ExecutorService ioExecutor;

    @Autowired
    public CrawlerService(ForkJoinPool crawlerForkJoinPool, ExecutorService ioExecutorService) {
        this.restTemplate = new RestTemplate();
        this.visitedUrls = ConcurrentHashMap.newKeySet();
        this.processingUrls = ConcurrentHashMap.newKeySet();
        this.activeTasks = new AtomicInteger(0);
        this.lock = new ReentrantLock();
        this.crawlerForkJoinPool = crawlerForkJoinPool;
        this.ioExecutor = ioExecutorService;

        startLoggingDaemon();
    }

    private void startLoggingDaemon() {
        Thread loggingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30000); // Каждые 30 секунд
                    logger.info("Crawler Status - Active tasks: {}, Visited URLs: {}, Processing URLs: {}",
                            activeTasks.get(), visitedUrls.size(), processingUrls.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "logging-daemon");

        loggingThread.setDaemon(true);
        loggingThread.start();
        logger.info("Logging daemon started");
    }

    @Async
    public void startCrawling(Set<String> startUrls, int maxDepth, int maxPages) {
        logger.info("Starting crawling with {} start URLs, max depth: {}, max pages: {}",
                startUrls.size(), maxDepth, maxPages);

        startUrls.forEach(url -> {
            if (visitedUrls.size() < maxPages && !visitedUrls.contains(url)) {
                crawlerForkJoinPool.submit(() -> crawlUrl(url, 0, maxDepth, maxPages));
            }
        });
    }

    private void crawlUrl(String url, int currentDepth, int maxDepth, int maxPages) {
        if (currentDepth > maxDepth || visitedUrls.size() >= maxPages || !processingUrls.add(url)) {
            return;
        }

        activeTasks.incrementAndGet();

        try {
            // Используем ExecutorService для асинхронных запросов вместо WebFlux
            ioExecutor.submit(() -> {
                try {
                    String htmlContent = fetchHtmlContent(url);
                    if (htmlContent != null) {
                        // Обрабатываем страницу в ForkJoinPool
                        crawlerForkJoinPool.submit(() -> {
                            processPage(url, htmlContent, currentDepth, maxDepth, maxPages);
                            activeTasks.decrementAndGet();
                            processingUrls.remove(url);
                        });
                    } else {
                        activeTasks.decrementAndGet();
                        processingUrls.remove(url);
                    }
                } catch (Exception e) {
                    logger.error("Error fetching URL: {} - {}", url, e.getMessage());
                    activeTasks.decrementAndGet();
                    processingUrls.remove(url);
                }
            });

        } catch (Exception e) {
            logger.error("Error processing URL: {} - {}", url, e.getMessage());
            activeTasks.decrementAndGet();
            processingUrls.remove(url);
        }
    }

    private String fetchHtmlContent(String url) {
        try {
            // Устанавливаем таймаут для запроса
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            logger.error("Failed to fetch URL: {} - {}", url, e.getMessage());
            return null;
        }
    }

    private Set<String> extractLinks(String htmlContent, String baseUrl) {
        Set<String> links = new HashSet<>();

        try {
            Document doc = Jsoup.parse(htmlContent, baseUrl);
            Elements linkElements = doc.select("a[href]");

            for (Element link : linkElements) {
                String href = link.attr("abs:href");
                if (isValidUrl(href)) {
                    links.add(href);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing HTML: {}", e.getMessage());
        }

        return links;
    }

    private boolean isValidUrl(String url) {
        return url != null &&
                !url.isEmpty() &&
                (url.startsWith("http://") || url.startsWith("https://")) &&
                !url.contains("#") &&
                !url.contains("mailto:") &&
                !url.contains("tel:") &&
                !url.contains("javascript:");
    }

    private void processPage(String url, String htmlContent, int currentDepth, int maxDepth, int maxPages) {
        try {
            // Извлечение контактной информации
            ContactInfo contactInfo = contactExtractorService.extractContactInfo(url, htmlContent);

            // Сохранение данных в H2
            storageService.saveContactInfo(contactInfo);

            logger.info("Processed: {} - Phones: {}, Emails: {}, Addresses: {}",
                    url, contactInfo.getPhones().size(),
                    contactInfo.getEmails().size(), contactInfo.getAddresses().size());

            // Извлечение ссылок для дальнейшего обхода
            if (currentDepth < maxDepth && visitedUrls.size() < maxPages) {
                Set<String> links = extractLinks(htmlContent, url);

                links.parallelStream()
                        .filter(link -> visitedUrls.size() < maxPages)
                        .filter(link -> visitedUrls.add(link))
                        .forEach(link -> {
                            crawlerForkJoinPool.submit(() ->
                                    crawlUrl(link, currentDepth + 1, maxDepth, maxPages));
                        });
            }

        } catch (Exception e) {
            logger.error("Error processing page {}: {}", url, e.getMessage());
        }
    }

    public Set<String> getVisitedUrls() {
        return Collections.unmodifiableSet(visitedUrls);
    }

    public int getActiveTasks() {
        return activeTasks.get();
    }

    public void shutdown() {
        crawlerForkJoinPool.shutdown();
        ioExecutor.shutdown();

        try {
            // Используем миллисекунды без TimeUnit
            Thread.sleep(60000); // Ждем 60 секунд

            // Проверяем, завершились ли пулы
            if (!crawlerForkJoinPool.isTerminated()) {
                crawlerForkJoinPool.shutdownNow();
            }
            if (!ioExecutor.isTerminated()) {
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            crawlerForkJoinPool.shutdownNow();
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}