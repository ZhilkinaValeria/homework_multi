package com.crawler.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ContactInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
    private Set<String> phones;
    private Set<String> emails;
    private Set<String> addresses;
    private long timestamp;
    private String title;

    public ContactInfo() {
        this.phones = ConcurrentHashMap.newKeySet();
        this.emails = ConcurrentHashMap.newKeySet();
        this.addresses = ConcurrentHashMap.newKeySet();
        this.timestamp = System.currentTimeMillis();
    }

    public ContactInfo(String url) {
        this();
        this.url = url;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Set<String> getPhones() { return phones; }
    public void setPhones(Set<String> phones) { this.phones = phones; }

    public Set<String> getEmails() { return emails; }
    public void setEmails(Set<String> emails) { this.emails = emails; }

    public Set<String> getAddresses() { return addresses; }
    public void setAddresses(Set<String> addresses) { this.addresses = addresses; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public void addPhone(String phone) {
        if (phone != null && !phone.trim().isEmpty()) {
            this.phones.add(phone.trim());
        }
    }

    public void addEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            this.emails.add(email.trim().toLowerCase());
        }
    }

    public void addAddress(String address) {
        if (address != null && !address.trim().isEmpty()) {
            this.addresses.add(address.trim());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactInfo that = (ContactInfo) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "ContactInfo{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", phones=" + phones.size() +
                ", emails=" + emails.size() +
                ", addresses=" + addresses.size() +
                ", timestamp=" + timestamp +
                '}';
    }
}