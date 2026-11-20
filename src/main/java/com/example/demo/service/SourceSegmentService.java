package com.example.demo.service;

import com.example.demo.model.SourceSegment;
import com.example.demo.repository.SourceSegmentRepository;
import com.example.demo.repository.TranslationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class SourceSegmentService {
    private final SourceSegmentRepository segments;
    private final TranslationRepository translations;
    private final TranslationProjectService projects;
    public SourceSegmentService(SourceSegmentRepository segments, TranslationRepository translations, TranslationProjectService projects) {
        this.segments = segments; this.translations = translations; this.projects = projects;
    }
    public List<SourceSegment> findAll() { return segments.findAll(); }
    public SourceSegment findById(Long id) { return segments.findById(id).orElseThrow(() -> Rules.missing("Segment", id)); }
    public SourceSegment create(SourceSegment segment) {
        Rules.require(segment.getProjectId() != null, "projectId is required");
        projects.editable(segment.getProjectId());
        Rules.text(segment.getSourceText(), "sourceText");
        Rules.require(segment.getOrder() != null && segment.getOrder() > 0, "order must be positive");
        segment.setId(null); segment.setCreatedAt(Instant.now());
        return segments.save(segment);
    }
    public SourceSegment update(Long id, SourceSegment patch) {
        SourceSegment segment = findById(id);
        projects.editable(segment.getProjectId());
        Rules.require(patch.getProjectId() == null || patch.getProjectId().equals(segment.getProjectId()), "Cannot move a segment to another project");
        if (patch.getSourceText() != null) {
            Rules.require(translations.findBySegmentId(id).isEmpty(), "Cannot change source text after translations were created");
            Rules.text(patch.getSourceText(), "sourceText"); segment.setSourceText(patch.getSourceText());
        }
        if (patch.getOrder() != null) { Rules.require(patch.getOrder() > 0, "order must be positive"); segment.setOrder(patch.getOrder()); }
        return segments.save(segment);
    }
    public void delete(Long id) {
        SourceSegment segment = findById(id);
        projects.editable(segment.getProjectId());
        segments.delete(segment);
    }
}
