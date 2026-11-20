package com.example.demo.controller;

import com.example.demo.model.SourceSegment;
import com.example.demo.service.SourceSegmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/source-segments")
public class SourceSegmentController {

    private final SourceSegmentService segmentService;

    public SourceSegmentController(SourceSegmentService segmentService) {
        this.segmentService = segmentService;
    }

    @GetMapping
    public ResponseEntity<List<SourceSegment>> getAll() {
        return ResponseEntity.ok(segmentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SourceSegment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(segmentService.findById(id));
    }

    @PostMapping
    @PreAuthorize("@access.project(#segment.projectId)")
    public ResponseEntity<SourceSegment> create(@RequestBody SourceSegment segment) {
        SourceSegment created = segmentService.create(segment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.segment(#id)")
    public ResponseEntity<SourceSegment> update(@PathVariable Long id, @RequestBody SourceSegment segment) {
        return ResponseEntity.ok(segmentService.update(id, segment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.segment(#id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        segmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

