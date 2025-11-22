package com.crawler.service;

import com.crawler.model.ContactInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ContactExtractorServiceTest {

    @Autowired
    private ContactExtractorService contactExtractorService;

    @Test
    void testExtractPhones() {
        String html = """
            <html>
                <body>
                    <p>Phone: +7 (999) 123-45-67</p>
                    <p>Another: 8-900-123-45-67</p>
                    <p>International: +1-234-567-8900</p>
                    <p>Invalid: 123-45-67</p>
                </body>
            </html>
            """;

        ContactInfo result = contactExtractorService.extractContactInfo("http://test.com", html);

        assertNotNull(result);
        assertFalse(result.getPhones().isEmpty());
        assertTrue(result.getPhones().contains("+79991234567"));
        assertTrue(result.getPhones().contains("+79001234567"));
        assertEquals(2, result.getPhones().size());
    }

    @Test
    void testExtractEmails() {
        String html = """
            <html>
                <body>
                    <p>Email: test@example.com</p>
                    <p>Contact: admin@site.ru</p>
                    <p>Support: support@company.org</p>
                    <p>Invalid: not-an-email</p>
                </body>
            </html>
            """;

        ContactInfo result = contactExtractorService.extractContactInfo("http://test.com", html);

        assertNotNull(result);
        assertEquals(3, result.getEmails().size());
        assertTrue(result.getEmails().contains("test@example.com"));
        assertTrue(result.getEmails().contains("admin@site.ru"));
        assertTrue(result.getEmails().contains("support@company.org"));
    }

    @Test
    void testExtractAddresses() {
        String html = """
            <html>
                <body>
                    <p>Address: г. Москва, ул. Тверская, д. 10</p>
                    <p>Location: город Санкт-Петербург, проспект Невский, 25</p>
                    <p>Invalid: просто текст без адреса</p>
                </body>
            </html>
            """;

        ContactInfo result = contactExtractorService.extractContactInfo("http://test.com", html);

        assertNotNull(result);
        assertFalse(result.getAddresses().isEmpty());
        assertTrue(result.getAddresses().size() >= 1);
    }

    @Test
    void testExtractTitle() {
        String html = """
            <html>
                <head>
                    <title>Test Company - Official Website</title>
                </head>
                <body>
                    <p>Some content</p>
                </body>
            </html>
            """;

        ContactInfo result = contactExtractorService.extractContactInfo("http://test.com", html);

        assertNotNull(result);
        assertEquals("Test Company - Official Website", result.getTitle());
    }

    @Test
    void testEmptyContent() {
        String html = "";

        ContactInfo result = contactExtractorService.extractContactInfo("http://test.com", html);

        assertNotNull(result);
        assertEquals("http://test.com", result.getUrl());
        assertTrue(result.getPhones().isEmpty());
        assertTrue(result.getEmails().isEmpty());
        assertTrue(result.getAddresses().isEmpty());
        assertEquals("No Title", result.getTitle());
    }

    @Test
    void testNullContent() {
        ContactInfo result = contactExtractorService.extractContactInfo("http://test.com", null);

        assertNotNull(result);
        assertEquals("http://test.com", result.getUrl());
        assertTrue(result.getPhones().isEmpty());
        assertTrue(result.getEmails().isEmpty());
        assertTrue(result.getAddresses().isEmpty());
        assertEquals("No Title", result.getTitle());
    }

    @Test
    void testPhoneNormalization() {
        String html = """
            <html>
                <body>
                    <p>+7 (999) 123-45-67</p>
                    <p>8(900)123-45-67</p>
                    <p>79991234567</p>
                </body>
            </html>
            """;

        ContactInfo result = contactExtractorService.extractContactInfo("http://test.com", html);

        assertNotNull(result);
        // Проверяем нормализацию номеров
        for (String phone : result.getPhones()) {
            assertTrue(phone.startsWith("+7"));
            assertEquals(12, phone.length()); // +7 + 10 цифр
        }
    }
}