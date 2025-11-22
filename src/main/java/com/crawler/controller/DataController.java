package com.crawler.controller;

import com.crawler.model.ContactInfo;
import com.crawler.service.H2StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataController {

    @Autowired
    private H2StorageService storageService;

    @GetMapping("/answer")
    public ResponseEntity<List<ContactInfo>> getAnswer() {
        List<ContactInfo> contacts = storageService.getAllContacts();
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<ContactInfo>> getAllContacts() {
        return ResponseEntity.ok(storageService.getAllContacts());
    }

    @GetMapping("/contacts/sorted")
    public ResponseEntity<List<ContactInfo>> getSortedContacts(
            @RequestParam String field,
            @RequestParam(defaultValue = "true") boolean ascending) {
        return ResponseEntity.ok(storageService.getContactsSortedBy(field, ascending));
    }

    @GetMapping("/contacts/filter")
    public ResponseEntity<List<ContactInfo>> filterContacts(@RequestParam String search) {
        return ResponseEntity.ok(storageService.filterContacts(search));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getDataCount() {
        return ResponseEntity.ok(Map.of("count", storageService.getDataCount()));
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearData() {
        storageService.clearData();
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }
}
