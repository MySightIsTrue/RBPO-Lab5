package com.example.demo.controller;

import com.example.demo.model.EditorialComment;
import com.example.demo.service.EditorialCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/editorial-comments")
public class EditorialCommentController {

    private final EditorialCommentService commentService;

    public EditorialCommentController(EditorialCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<List<EditorialComment>> getAll() {
        return ResponseEntity.ok(commentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EditorialComment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.findById(id));
    }

    @PostMapping
    @PreAuthorize("@access.review(#comment.translationId)")
    public ResponseEntity<EditorialComment> create(@RequestBody EditorialComment comment) {
        EditorialComment created = commentService.create(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.commentReview(#id)")
    public ResponseEntity<EditorialComment> update(@PathVariable Long id, @RequestBody EditorialComment comment) {
        return ResponseEntity.ok(commentService.update(id, comment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.commentReview(#id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

