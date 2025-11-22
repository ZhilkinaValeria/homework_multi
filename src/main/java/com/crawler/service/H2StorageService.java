package com.crawler.service;

import com.crawler.model.ContactInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class H2StorageService {

    private final ReadWriteLock lock;
    private final Set<ContactInfo> memoryCache;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public H2StorageService() {
        this.lock = new ReentrantReadWriteLock();
        this.memoryCache = ConcurrentHashMap.newKeySet();
    }

    // Инициализация после создания бина
    @Autowired
    public void initialize() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS contact_info (
                    url VARCHAR(1000) PRIMARY KEY,
                    title VARCHAR(500),
                    timestamp BIGINT,
                    phones CLOB,
                    emails CLOB,
                    addresses CLOB
                )
            """);

            loadDataToCache();
            System.out.println("Database initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDataToCache() {
        try {
            List<ContactInfo> contacts = jdbcTemplate.query(
                    "SELECT url, title, timestamp, phones, emails, addresses FROM contact_info",
                    getContactInfoRowMapper()
            );
            memoryCache.addAll(contacts);
            System.out.println("Loaded " + contacts.size() + " contacts from H2 database");
        } catch (Exception e) {
            System.err.println("Error loading data from database: " + e.getMessage());
        }
    }

    public void saveContactInfo(ContactInfo contactInfo) {
        lock.writeLock().lock();
        try {
            memoryCache.remove(contactInfo);
            memoryCache.add(contactInfo);

            jdbcTemplate.update("""
                MERGE INTO contact_info (url, title, timestamp, phones, emails, addresses) 
                KEY(url) 
                VALUES (?, ?, ?, ?, ?, ?)
            """,
                    contactInfo.getUrl(),
                    contactInfo.getTitle(),
                    contactInfo.getTimestamp(),
                    setToString(contactInfo.getPhones()),
                    setToString(contactInfo.getEmails()),
                    setToString(contactInfo.getAddresses()));

        } catch (Exception e) {
            System.err.println("Error saving contact info: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<ContactInfo> getAllContacts() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(memoryCache);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<ContactInfo> getContactsSortedBy(String field, boolean ascending) {
        List<ContactInfo> contacts = getAllContacts();

        return contacts.parallelStream()
                .sorted((c1, c2) -> {
                    int result = 0;
                    switch (field.toLowerCase()) {
                        case "url":
                            result = c1.getUrl().compareTo(c2.getUrl());
                            break;
                        case "title":
                            result = c1.getTitle().compareTo(c2.getTitle());
                            break;
                        case "phones":
                            result = Integer.compare(c1.getPhones().size(), c2.getPhones().size());
                            break;
                        case "emails":
                            result = Integer.compare(c1.getEmails().size(), c2.getEmails().size());
                            break;
                        case "timestamp":
                            result = Long.compare(c1.getTimestamp(), c2.getTimestamp());
                            break;
                        default:
                            result = c1.getUrl().compareTo(c2.getUrl());
                    }
                    return ascending ? result : -result;
                })
                .toList();
    }

    public List<ContactInfo> filterContacts(String searchTerm) {
        List<ContactInfo> contacts = getAllContacts();
        final String term = searchTerm.toLowerCase();

        return contacts.parallelStream()
                .filter(contact ->
                        contact.getUrl().toLowerCase().contains(term) ||
                                (contact.getTitle() != null && contact.getTitle().toLowerCase().contains(term)) ||
                                contact.getPhones().stream().anyMatch(phone -> phone.contains(term)) ||
                                contact.getEmails().stream().anyMatch(email -> email.contains(term)) ||
                                contact.getAddresses().stream().anyMatch(address -> address.toLowerCase().contains(term))
                )
                .toList();
    }

    public int getDataCount() {
        lock.readLock().lock();
        try {
            return memoryCache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clearData() {
        lock.writeLock().lock();
        try {
            jdbcTemplate.update("DELETE FROM contact_info");
            memoryCache.clear();
            System.out.println("All data cleared");
        } catch (Exception e) {
            System.err.println("Error clearing data: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String setToString(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return "";
        }
        return String.join(";;", set);
    }

    private Set<String> stringToSet(String str) {
        if (str == null || str.trim().isEmpty()) {
            return ConcurrentHashMap.newKeySet();
        }
        Set<String> result = ConcurrentHashMap.newKeySet();
        String[] parts = str.split(";;");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private RowMapper<ContactInfo> getContactInfoRowMapper() {
        return new RowMapper<ContactInfo>() {
            @Override
            public ContactInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
                ContactInfo contactInfo = new ContactInfo(rs.getString("url"));
                contactInfo.setTitle(rs.getString("title"));
                contactInfo.setTimestamp(rs.getLong("timestamp"));
                contactInfo.setPhones(stringToSet(rs.getString("phones")));
                contactInfo.setEmails(stringToSet(rs.getString("emails")));
                contactInfo.setAddresses(stringToSet(rs.getString("addresses")));
                return contactInfo;
            }
        };
    }
}