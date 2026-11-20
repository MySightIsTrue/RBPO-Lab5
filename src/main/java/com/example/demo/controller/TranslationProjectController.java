package com.example.demo.controller;

import com.example.demo.model.TranslationProject;
import com.example.demo.service.TranslationProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/translation-projects")
public class TranslationProjectController {

    private final TranslationProjectService projectService;

    public TranslationProjectController(TranslationProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<TranslationProject>> getAll() {
        return ResponseEntity.ok(projectService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TranslationProject> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @PostMapping
    @PreAuthorize("@access.editor() and @access.self(#project.ownerId)")
    public ResponseEntity<TranslationProject> create(@RequestBody TranslationProject project) {
        TranslationProject created = projectService.create(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.project(#id) and (#project.ownerId == null or @access.self(#project.ownerId))")
    public ResponseEntity<TranslationProject> update(@PathVariable Long id, @RequestBody TranslationProject project) {
        return ResponseEntity.ok(projectService.update(id, project));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.project(#id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

