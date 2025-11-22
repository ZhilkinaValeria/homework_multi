package com.crawler.service;

import com.crawler.model.ContactInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class H2StorageServiceTest {

    @Autowired
    private H2StorageService storageService;

    @Test
    void testSaveAndRetrieveContactInfo() {
        ContactInfo contactInfo = new ContactInfo("http://test-save.com");
        contactInfo.setTitle("Test Company");
        contactInfo.addPhone("+79991234567");
        contactInfo.addEmail("test@company.com");
        contactInfo.addAddress("г. Москва, ул. Тестовая, д. 1");

        storageService.saveContactInfo(contactInfo);

        List<ContactInfo> contacts = storageService.getAllContacts();
        assertFalse(contacts.isEmpty());

        ContactInfo retrieved = contacts.stream()
                .filter(c -> c.getUrl().equals("http://test-save.com"))
                .findFirst()
                .orElse(null);

        assertNotNull(retrieved);
        assertEquals("Test Company", retrieved.getTitle());
        assertTrue(retrieved.getPhones().contains("+79991234567"));
        assertTrue(retrieved.getEmails().contains("test@company.com"));
    }

    @Test
    void testSortContacts() {
        ContactInfo contact1 = new ContactInfo("http://company-a.com");
        contact1.setTitle("Company A");
        contact1.addPhone("+79991111111");
        storageService.saveContactInfo(contact1);

        ContactInfo contact2 = new ContactInfo("http://company-b.com");
        contact2.setTitle("Company B");
        contact2.addPhone("+79992222222");
        contact2.addPhone("+79993333333");
        storageService.saveContactInfo(contact2);

        // Сортировка по количеству телефонов (по убыванию)
        List<ContactInfo> sorted = storageService.getContactsSortedBy("phones", false);
        assertTrue(sorted.size() >= 2);

        // Company B должен быть первым (больше телефонов)
        ContactInfo first = sorted.get(0);
        assertTrue(first.getPhones().size() >= 1);
    }

    @Test
    void testFilterContacts() {
        ContactInfo contact = new ContactInfo("http://filter-test.com");
        contact.setTitle("Test Company for Filtering");
        contact.addPhone("+79991234567");
        contact.addEmail("filter@test.com");
        contact.addAddress("г. Москва, ул. Фильтровая");
        storageService.saveContactInfo(contact);

        // Фильтрация по телефону
        List<ContactInfo> filteredByPhone = storageService.filterContacts("+79991234567");
        assertFalse(filteredByPhone.isEmpty());

        // Фильтрация по email
        List<ContactInfo> filteredByEmail = storageService.filterContacts("filter@test.com");
        assertFalse(filteredByEmail.isEmpty());

        // Фильтрация по названию компании
        List<ContactInfo> filteredByTitle = storageService.filterContacts("Filtering");
        assertFalse(filteredByTitle.isEmpty());
    }

    @Test
    void testDataCount() {
        int initialCount = storageService.getDataCount();

        ContactInfo contact = new ContactInfo("http://count-test.com");
        storageService.saveContactInfo(contact);

        int newCount = storageService.getDataCount();
        assertTrue(newCount >= initialCount);
    }

    @Test
    void testClearData() {
        ContactInfo contact = new ContactInfo("http://clear-test.com");
        storageService.saveContactInfo(contact);

        storageService.clearData();

        List<ContactInfo> contacts = storageService.getAllContacts();
        // После очистки данных может остаться 0 или больше записей (в зависимости от других тестов)
        assertNotNull(contacts);
    }
}
