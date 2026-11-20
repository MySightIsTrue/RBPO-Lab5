package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.WorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api")
public class WorkflowController {
    private final WorkflowService workflow;
    public WorkflowController(WorkflowService workflow) { this.workflow = workflow; }
    public record RevisionRequest(String commentText) {}
    public record ResolveRequest(String text) {}

    @PostMapping("/translations/{id}/submit")
    @PreAuthorize("@access.author(#id)")
    public Translation submit(@PathVariable Long id) { return workflow.submit(id); }

    @PostMapping("/translations/{id}/request-revision")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@access.review(#id)")
    public EditorialComment requestRevision(@PathVariable Long id, @RequestBody RevisionRequest body) {
        return workflow.requestRevision(id, body.commentText());
    }

    @PostMapping("/editorial-comments/{id}/resolve")
    @PreAuthorize("@access.commentAuthor(#id)")
    public EditorialComment resolve(@PathVariable Long id, @RequestBody ResolveRequest body) {
        return workflow.resolve(id, body.text());
    }

    @PostMapping("/translations/{id}/approve")
    @PreAuthorize("@access.review(#id)")
    public Translation approve(@PathVariable Long id) { return workflow.approve(id); }

    @PostMapping("/translation-projects/{id}/complete")
    @PreAuthorize("@access.project(#id)")
    public TranslationProject complete(@PathVariable Long id) { return workflow.complete(id); }
}

