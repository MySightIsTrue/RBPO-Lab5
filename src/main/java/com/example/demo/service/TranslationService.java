package com.example.demo.service;

import com.example.demo.model.Translation;
import com.example.demo.repository.TranslationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class TranslationService {
    private final TranslationRepository translations;
    private final SourceSegmentService segments;
    private final TranslationProjectService projects;
    private final UserService users;
    public TranslationService(TranslationRepository translations, SourceSegmentService segments, TranslationProjectService projects, UserService users) {
        this.translations = translations; this.segments = segments; this.projects = projects; this.users = users;
    }
    public List<Translation> findAll() { return translations.findAll(); }
    public Translation findById(Long id) { return translations.findById(id).orElseThrow(() -> Rules.missing("Translation", id)); }
    public Translation active(Long id) {
        Translation translation = findById(id);
        projects.editable(segments.findById(translation.getSegmentId()).getProjectId());
        return translation;
    }
    public Translation create(Translation translation) {
        Rules.require(translation.getSegmentId() != null && translation.getAuthorId() != null, "segmentId and authorId are required");
        projects.editable(segments.findById(translation.getSegmentId()).getProjectId());
        users.findById(translation.getAuthorId());
        Rules.text(translation.getText(), "text");
        Rules.initialStatus(translation.getStatus(), "DRAFT");
        translation.setId(null); translation.setStatus("DRAFT");
        translation.setCreatedAt(Instant.now()); translation.setUpdatedAt(translation.getCreatedAt());
        return translations.save(translation);
    }
    public Translation update(Long id, Translation patch) {
        Translation translation = active(id);
        Rules.require("DRAFT".equals(translation.getStatus()) || "REVISION".equals(translation.getStatus()), "Only a draft or revision can be edited");
        Rules.require(patch.getSegmentId() == null || patch.getSegmentId().equals(translation.getSegmentId()), "Cannot move a translation");
        Rules.require(patch.getAuthorId() == null || patch.getAuthorId().equals(translation.getAuthorId()), "Cannot change author");
        Rules.require(patch.getStatus() == null || patch.getStatus().equals(translation.getStatus()), "Use business operations to change status");
        if (patch.getText() != null) { Rules.text(patch.getText(), "text"); translation.setText(patch.getText()); }
        translation.setUpdatedAt(Instant.now());
        return translations.save(translation);
    }
    public void delete(Long id) {
        Translation translation = active(id);
        Rules.require(!"APPROVED".equals(translation.getStatus()), "Approved translation cannot be deleted");
        translations.delete(translation);
    }
}
