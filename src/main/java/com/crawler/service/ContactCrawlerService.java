package com.crawler.service;

import com.crawler.model.ContactData;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ContactCrawlerService {

    // Встроенное хранилище данных
    private final List<ContactData> contactStorage;
    private final Map<Long, ContactData> contactMap;

    // Пул потоков
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduler;

    // Потокобезопасные коллекции
    private final Set<String> visitedUrls;
    private final AtomicInteger activeTasks;
    private final Object storageLock = new Object();

    // Регулярные выражения для поиска данных
    private final Pattern phonePattern;
    private final Pattern emailPattern;
    private final Pattern linkPattern;
    private final Pattern titlePattern;
    private final Pattern h1Pattern;

    // Статистика
    private final AtomicInteger totalProcessed;
    private final AtomicInteger totalFound;

    public ContactCrawlerService() {
        this.contactStorage = Collections.synchronizedList(new ArrayList<>());
        this.contactMap = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(10);
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.visitedUrls = ConcurrentHashMap.newKeySet();
        this.activeTasks = new AtomicInteger(0);
        this.totalProcessed = new AtomicInteger(0);
        this.totalFound = new AtomicInteger(0);

        // Инициализация регулярных выражений
        this.phonePattern = Pattern.compile("(\\+7|8)[\\s\\-\\(\\)\\d]{10,15}");
        this.emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        this.linkPattern = Pattern.compile("href=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
        this.titlePattern = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE);
        this.h1Pattern = Pattern.compile("<h1[^>]*>(.*?)</h1>", Pattern.CASE_INSENSITIVE);

        startScheduledTasks();
        loadFromFile();
    }

    // ========== ПУБЛИЧНЫЕ МЕТОДЫ ==========

    /**
     * Запуск краулера с использованием простых потоков (Thread, Runnable)
     */
    public void startBasicCrawling(List<String> startUrls) {
        System.out.println("Запуск базового краулера с " + startUrls.size() + " URL");

        for (String url : startUrls) {
            Thread crawlerThread = new Thread(new BasicCrawlerTask(url, 1));
            crawlerThread.setName("basic-crawler-" + url.hashCode());
            crawlerThread.start();
        }
    }

    /**
     * Запуск продвинутого краулера с ExecutorService
     */
    public CompletableFuture<Map<String, Object>> startAdvancedCrawling(List<String> startUrls, int maxDepth) {
        System.out.println("Запуск продвинутого краулера с глубиной " + maxDepth);

        List<CompletableFuture<Void>> futures = startUrls.stream()
                .map(url -> CompletableFuture.runAsync(() ->
                        crawlWebsite(url, maxDepth), executorService))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "completed");
                    result.put("processedUrls", totalProcessed.get());
                    result.put("foundContacts", totalFound.get());
                    result.put("totalStorage", contactStorage.size());
                    return result;
                });
    }

    /**
     * Поиск контактов с использованием Parallel Stream
     */
    public List<ContactData> searchContacts(String query, String sortBy) {
        List<ContactData> allContacts;
        synchronized (storageLock) {
            allContacts = new ArrayList<>(contactStorage);
        }

        return allContacts.parallelStream()
                .filter(contact -> containsQuery(contact, query))
                .sorted(getComparator(sortBy))
                .collect(Collectors.toList());
    }

    /**
     * Получение статистики
     */
    public Map<String, Object> getStats() {
        List<ContactData> allContacts;
        synchronized (storageLock) {
            allContacts = new ArrayList<>(contactStorage);
        }

        long withPhones = allContacts.parallelStream()
                .filter(contact -> contact.getPhones() != null && !contact.getPhones().isEmpty())
                .count();

        long withEmails = allContacts.parallelStream()
                .filter(contact -> contact.getEmails() != null && !contact.getEmails().isEmpty())
                .count();

        long withAddresses = allContacts.parallelStream()
                .filter(contact -> contact.getAddresses() != null && !contact.getAddresses().isEmpty())
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", allContacts.size());
        stats.put("withPhones", withPhones);
        stats.put("withEmails", withEmails);
        stats.put("withAddresses", withAddresses);
        stats.put("activeTasks", activeTasks.get());
        stats.put("visitedUrls", visitedUrls.size());
        stats.put("totalProcessed", totalProcessed.get());
        stats.put("totalFound", totalFound.get());

        return stats;
    }

    /**
     * Сохранение данных в файл
     */
    public void saveToFile() {
        synchronized (storageLock) {
            try (FileWriter writer = new FileWriter("contact_data.txt", false)) {
                for (ContactData contact : contactStorage) {
                    writer.write(serializeContact(contact) + "\n");
                }
                System.out.println("Данные сохранены в файл: " + contactStorage.size() + " записей");
            } catch (Exception e) {
                System.err.println("Ошибка при сохранении в файл: " + e.getMessage());
            }
        }
    }

    // ========== ПРИВАТНЫЕ МЕТОДЫ ==========

    /**
     * Задача для простого потока (Runnable)
     */
    private class BasicCrawlerTask implements Runnable {
        private final String startUrl;
        private final int maxDepth;

        public BasicCrawlerTask(String startUrl, int maxDepth) {
            this.startUrl = startUrl;
            this.maxDepth = maxDepth;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " запущен для: " + startUrl);
            crawlWebsite(startUrl, maxDepth);
        }
    }

    /**
     * Основной метод обхода веб-сайта
     */
    private void crawlWebsite(String url, int maxDepth) {
        if (visitedUrls.contains(url) || maxDepth < 0) {
            return;
        }

        visitedUrls.add(url);
        activeTasks.incrementAndGet();
        totalProcessed.incrementAndGet();

        try {
            String htmlContent = fetchUrlContent(url);

            if (htmlContent != null) {
                // Извлечение данных
                String companyName = extractCompanyName(htmlContent);
                String phones = extractPhones(htmlContent);
                String emails = extractEmails(htmlContent);
                String addresses = extractAddresses(htmlContent);

                // Сохранение если найдены данные
                if (phones != null || emails != null || addresses != null) {
                    saveContactData(url, companyName, phones, emails, addresses);
                    totalFound.incrementAndGet();
                }

                // Рекурсивный обход ссылок
                if (maxDepth > 0) {
                    List<String> links = extractLinks(htmlContent, url);
                    processLinksRecursively(links, maxDepth - 1);
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка при обработке URL " + url + ": " + e.getMessage());
        } finally {
            activeTasks.decrementAndGet();
        }
    }

    /**
     * Рекурсивная обработка ссылок
     */
    private void processLinksRecursively(List<String> links, int depth) {
        if (depth <= 0 || links.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures = links.stream()
                .filter(link -> !visitedUrls.contains(link))
                .map(link -> CompletableFuture.runAsync(() ->
                        crawlWebsite(link, depth), executorService))
                .collect(Collectors.toList());

        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    /**
     * HTTP клиент для получения содержимого страницы
     */
    private String fetchUrlContent(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                return content.toString();
            } else {
                System.err.println("HTTP ошибка " + responseCode + " для URL: " + urlString);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Ошибка при получении содержимого " + urlString + ": " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Извлечение названия компании
     */
    private String extractCompanyName(String html) {
        // Поиск в title
        String title = extractByPattern(html, titlePattern);
        if (title != null && !title.isEmpty()) {
            return title.split("[\\-\\|]")[0].trim();
        }

        // Поиск в h1
        String h1 = extractByPattern(html, h1Pattern);
        if (h1 != null && !h1.isEmpty()) {
            // Удаляем HTML теги из h1
            return h1.replaceAll("<[^>]+>", "").trim();
        }

        return "Неизвестная компания";
    }

    /**
     * Извлечение телефонов
     */
    private String extractPhones(String html) {
        String text = html.replaceAll("<[^>]+>", " ");
        java.util.regex.Matcher matcher = phonePattern.matcher(text);
        Set<String> phones = new HashSet<>();

        while (matcher.find()) {
            phones.add(matcher.group());
        }

        return phones.isEmpty() ? null : String.join("; ", phones);
    }

    /**
     * Извлечение email адресов
     */
    private String extractEmails(String html) {
        String text = html.replaceAll("<[^>]+>", " ");
        java.util.regex.Matcher matcher = emailPattern.matcher(text);
        Set<String> emails = new HashSet<>();

        while (matcher.find()) {
            emails.add(matcher.group());
        }

        // Поиск в mailto ссылках
        Pattern mailtoPattern = Pattern.compile("href=\"mailto:([^\"]+)\"");
        java.util.regex.Matcher mailtoMatcher = mailtoPattern.matcher(html);
        while (mailtoMatcher.find()) {
            String email = mailtoMatcher.group(1);
            emails.add(email);
        }

        return emails.isEmpty() ? null : String.join("; ", emails);
    }

    /**
     * Извлечение адресов
     */
    private String extractAddresses(String html) {
        String text = html.replaceAll("<[^>]+>", " ");
        String[] addressKeywords = {"ул.", "улица", "проспект", "пр.", "дом", "д.", "г.", "город", "address", "адрес"};

        for (String keyword : addressKeywords) {
            int index = text.toLowerCase().indexOf(keyword.toLowerCase());
            if (index != -1) {
                int start = Math.max(0, index - 50);
                int end = Math.min(text.length(), index + 150);
                String address = text.substring(start, end)
                        .replaceAll("\\s+", " ")
                        .trim();
                return address.length() > 200 ? address.substring(0, 200) + "..." : address;
            }
        }

        return null;
    }

    /**
     * Извлечение ссылок со страницы
     */
    private List<String> extractLinks(String html, String baseUrl) {
        List<String> links = new ArrayList<>();
        java.util.regex.Matcher matcher = linkPattern.matcher(html);

        try {
            URL base = new URL(baseUrl);

            while (matcher.find()) {
                String href = matcher.group(1);
                String fullUrl = convertToFullUrl(href, base);
                if (fullUrl != null && isValidUrl(fullUrl)) {
                    links.add(fullUrl);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при извлечении ссылок: " + e.getMessage());
        }

        return links;
    }

    /**
     * Преобразование относительных ссылок в абсолютные
     */
    private String convertToFullUrl(String href, URL base) {
        try {
            if (href.startsWith("http")) {
                return href;
            } else if (href.startsWith("/")) {
                return base.getProtocol() + "://" + base.getHost() + href;
            } else if (href.startsWith("./")) {
                return base.getProtocol() + "://" + base.getHost() + base.getPath().replaceAll("[^/]*$", "") + href.substring(2);
            } else {
                return base.getProtocol() + "://" + base.getHost() + base.getPath().replaceAll("[^/]*$", "") + href;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Валидация URL
     */
    private boolean isValidUrl(String url) {
        return url != null &&
                url.startsWith("http") &&
                !url.contains("#") &&
                !url.matches(".*\\.(pdf|doc|docx|jpg|png|gif|zip|rar)$") &&
                url.length() < 500;
    }

    /**
     * Сохранение контактных данных
     */
    private void saveContactData(String url, String companyName, String phones, String emails, String addresses) {
        Long newId = System.currentTimeMillis();
        ContactData contactData = new ContactData(newId, url, companyName, phones, emails, addresses);

        synchronized (storageLock) {
            contactStorage.add(contactData);
            contactMap.put(newId, contactData);
        }

        System.out.println("Сохранены данные: " + companyName + " [" + url + "]");
    }

    /**
     * Вспомогательный метод для извлечения данных по регулярному выражению
     */
    private String extractByPattern(String text, Pattern pattern) {
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Компаратор для сортировки
     */
    private Comparator<ContactData> getComparator(String sortBy) {
        switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "company":
                return Comparator.comparing(ContactData::getCompanyName,
                        Comparator.nullsLast(String::compareToIgnoreCase));
            case "url":
                return Comparator.comparing(ContactData::getUrl,
                        Comparator.nullsLast(String::compareToIgnoreCase));
            case "date":
                return Comparator.comparing(ContactData::getCollectedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()));
            default:
                return Comparator.comparing(ContactData::getId);
        }
    }

    /**
     * Поиск по запросу
     */
    private boolean containsQuery(ContactData contact, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        String lowerQuery = query.toLowerCase();
        return (contact.getCompanyName() != null && contact.getCompanyName().toLowerCase().contains(lowerQuery)) ||
                (contact.getPhones() != null && contact.getPhones().toLowerCase().contains(lowerQuery)) ||
                (contact.getEmails() != null && contact.getEmails().toLowerCase().contains(lowerQuery)) ||
                (contact.getAddresses() != null && contact.getAddresses().toLowerCase().contains(lowerQuery)) ||
                (contact.getUrl() != null && contact.getUrl().toLowerCase().contains(lowerQuery));
    }

    /**
     * Планировщик задач
     */
    private void startScheduledTasks() {
        // Автосохранение каждые 10 минут
        scheduler.scheduleAtFixedRate(() -> {
            saveToFile();
        }, 10, 10, TimeUnit.MINUTES);

        // Логирование статуса каждую минуту
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("=== СТАТУС КРАУЛЕРА ===");
            System.out.println("Активных задач: " + activeTasks.get());
            System.out.println("Посещено URL: " + visitedUrls.size());
            System.out.println("Всего обработано: " + totalProcessed.get());
            System.out.println("Найдено контактов: " + totalFound.get());
            System.out.println("В хранилище: " + contactStorage.size() + " записей");
            System.out.println("======================");
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Загрузка данных из файла
     */
    private void loadFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("contact_data.txt"))) {
            String line;
            int loadedCount = 0;

            while ((line = reader.readLine()) != null) {
                ContactData contact = deserializeContact(line);
                if (contact != null) {
                    synchronized (storageLock) {
                        contactStorage.add(contact);
                        contactMap.put(contact.getId(), contact);
                    }
                    loadedCount++;
                }
            }
            System.out.println("Загружено " + loadedCount + " записей из файла");
        } catch (Exception e) {
            System.out.println("Файл с данными не найден, будет создан новый");
        }
    }

    /**
     * Сериализация контакта в строку
     */
    private String serializeContact(ContactData contact) {
        return String.join("|",
                contact.getId().toString(),
                contact.getUrl() != null ? contact.getUrl() : "",
                contact.getCompanyName() != null ? contact.getCompanyName() : "",
                contact.getPhones() != null ? contact.getPhones() : "",
                contact.getEmails() != null ? contact.getEmails() : "",
                contact.getAddresses() != null ? contact.getAddresses() : "",
                contact.getCollectedAt().toString()
        );
    }

    /**
     * Десериализация контакта из строки
     */
    private ContactData deserializeContact(String line) {
        try {
            String[] parts = line.split("\\|", 7);
            if (parts.length == 7) {
                ContactData contact = new ContactData();
                contact.setId(Long.parseLong(parts[0]));
                contact.setUrl(parts[1]);
                contact.setCompanyName(parts[2]);
                contact.setPhones(parts[3]);
                contact.setEmails(parts[4]);
                contact.setAddresses(parts[5]);
                contact.setCollectedAt(LocalDateTime.parse(parts[6]));
                return contact;
            }
        } catch (Exception e) {
            System.err.println("Ошибка при десериализации: " + e.getMessage());
        }
        return null;
    }

    /**
     * Завершение работы сервиса
     */
    public void shutdown() {
        saveToFile();

        executorService.shutdown();
        scheduler.shutdown();

        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}