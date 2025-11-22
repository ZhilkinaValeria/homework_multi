package com.crawler.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class ContactData {
    private Long id;
    private String url;
    private String companyName;
    private String phones;
    private String emails;
    private String addresses;
    private LocalDateTime collectedAt;

    public ContactData() {
        this.collectedAt = LocalDateTime.now();
    }

    public ContactData(Long id, String url, String companyName, String phones,
                       String emails, String addresses) {
        this.id = id;
        this.url = url;
        this.companyName = companyName;
        this.phones = phones;
        this.emails = emails;
        this.addresses = addresses;
        this.collectedAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getPhones() { return phones; }
    public void setPhones(String phones) { this.phones = phones; }

    public String getEmails() { return emails; }
    public void setEmails(String emails) { this.emails = emails; }

    public String getAddresses() { return addresses; }
    public void setAddresses(String addresses) { this.addresses = addresses; }

    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactData that = (ContactData) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "ContactData{id=%d, url='%s', companyName='%s', phones='%s', emails='%s', addresses='%s', collectedAt=%s}",
                id, url, companyName, phones, emails, addresses, collectedAt
        );
    }
}