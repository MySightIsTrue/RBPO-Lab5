package com.example.demo.controller;

import com.example.demo.model.Translation;
import com.example.demo.service.TranslationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/translations")
public class TranslationController {

    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @GetMapping
    public ResponseEntity<List<Translation>> getAll() {
        return ResponseEntity.ok(translationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Translation> getById(@PathVariable Long id) {
        return ResponseEntity.ok(translationService.findById(id));
    }

    @PostMapping
    @PreAuthorize("@access.self(#translation.authorId)")
    public ResponseEntity<Translation> create(@RequestBody Translation translation) {
        Translation created = translationService.create(translation);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.author(#id)")
    public ResponseEntity<Translation> update(@PathVariable Long id, @RequestBody Translation translation) {
        return ResponseEntity.ok(translationService.update(id, translation));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.author(#id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        translationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

