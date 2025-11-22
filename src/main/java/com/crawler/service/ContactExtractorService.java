package com.crawler.service;

import com.crawler.model.ContactInfo;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContactExtractorService {

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(\\+7|8|7)[\\s\\-\\(\\)]*(\\d[\\s\\-\\(\\)]*){9,10}"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );

    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "([гГ]\\.?\\s*[а-яА-ЯёЁ\\-]+|[гГ][оО][рР][оО][дД]\\s+[а-яА-ЯёЁ\\-]+)," +
                    "?\\s*([уУ][лЛ]\\.?\\s*[а-яА-ЯёЁ\\-]+|[пП][рР][оО][сС][пП][еЕ][кК][тТ]\\s+[а-яА-ЯёЁ\\-]+)," +
                    "?\\s*(\\d+[а-яА-Я]?)?"
    );

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<title>(.*?)</title>", Pattern.CASE_INSENSITIVE
    );

    public ContactInfo extractContactInfo(String url, String htmlContent) {
        ContactInfo contactInfo = new ContactInfo(url);

        String title = extractTitle(htmlContent);
        contactInfo.setTitle(title);

        Set<String> phones = extractPhones(htmlContent);
        phones.forEach(contactInfo::addPhone);

        Set<String> emails = extractEmails(htmlContent);
        emails.forEach(contactInfo::addEmail);

        Set<String> addresses = extractAddresses(htmlContent);
        addresses.forEach(contactInfo::addAddress);

        return contactInfo;
    }

    private String extractTitle(String htmlContent) {
        Matcher matcher = TITLE_PATTERN.matcher(htmlContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "No Title";
    }

    private Set<String> extractPhones(String text) {
        Set<String> phones = new HashSet<>();
        Matcher matcher = PHONE_PATTERN.matcher(text);

        while (matcher.find()) {
            String phone = normalizePhone(matcher.group());
            if (isValidPhone(phone)) {
                phones.add(phone);
            }
        }

        return phones;
    }

    private String normalizePhone(String phone) {
        String cleaned = phone.replaceAll("[^\\d+]", "");

        if (cleaned.startsWith("8") && cleaned.length() == 11) {
            return "+7" + cleaned.substring(1);
        } else if (cleaned.startsWith("7") && cleaned.length() == 11) {
            return "+" + cleaned;
        } else if (cleaned.startsWith("+7") && cleaned.length() == 12) {
            return cleaned;
        }

        return cleaned;
    }

    private boolean isValidPhone(String phone) {
        return phone != null &&
                ((phone.startsWith("+7") && phone.length() == 12) ||
                        (phone.startsWith("8") && phone.length() == 11));
    }

    private Set<String> extractEmails(String text) {
        Set<String> emails = new HashSet<>();
        Matcher matcher = EMAIL_PATTERN.matcher(text);

        while (matcher.find()) {
            emails.add(matcher.group().toLowerCase());
        }

        return emails;
    }

    private Set<String> extractAddresses(String text) {
        Set<String> addresses = new HashSet<>();
        Matcher matcher = ADDRESS_PATTERN.matcher(text);

        while (matcher.find()) {
            String address = matcher.group().trim();
            if (address.length() > 10) {
                addresses.add(address);
            }
        }

        return addresses;
    }
}