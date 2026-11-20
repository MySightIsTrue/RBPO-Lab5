package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class WorkflowService {
    private final TranslationService translations;
    private final TranslationProjectService projects;
    private final SourceSegmentService segments;
    private final EditorialCommentService comments;
    private final TranslationRepository translationRepository;
    private final SourceSegmentRepository segmentRepository;
    private final EditorialCommentRepository commentRepository;

    public WorkflowService(TranslationService translations, TranslationProjectService projects,
                           SourceSegmentService segments, EditorialCommentService comments,
                           TranslationRepository translationRepository, SourceSegmentRepository segmentRepository,
                           EditorialCommentRepository commentRepository) {
        this.translations = translations; this.projects = projects; this.segments = segments;
        this.comments = comments; this.translationRepository = translationRepository;
        this.segmentRepository = segmentRepository; this.commentRepository = commentRepository;
    }

    // 1. Отправка перевода запускает работу над проектом.
    public Translation submit(Long id) {
        Translation translation = translations.active(id);
        Rules.require(List.of("DRAFT", "REVISION").contains(translation.getStatus()), "Only a draft or revision can be submitted");
        Rules.text(translation.getText(), "text");
        Rules.require(!hasOpenComments(id), "Resolve all comments before submission");
        Rules.require(translationRepository.findBySegmentId(translation.getSegmentId()).stream()
                .noneMatch(t -> "APPROVED".equals(t.getStatus())), "Segment already has an approved translation");
        TranslationProject project = projects.editable(segments.findById(translation.getSegmentId()).getProjectId());
        project.setStatus("IN_PROGRESS"); project.setUpdatedAt(Instant.now());
        translation.setStatus("SUBMITTED"); translation.setUpdatedAt(Instant.now());
        return translation;
    }

    // 2. Возврат на доработку обязательно сопровождается новым замечанием.
    public EditorialComment requestRevision(Long id, String text) {
        Translation translation = translations.active(id);
        Rules.require("SUBMITTED".equals(translation.getStatus()), "Only a submitted translation can be returned for revision");
        EditorialComment comment = new EditorialComment();
        comment.setTranslationId(id); comment.setCommentText(text);
        return comments.create(comment);
    }

    // 3. Замечание закрывается только вместе с исправлением текста перевода.
    public EditorialComment resolve(Long id, String correctedText) {
        EditorialComment comment = comments.findById(id);
        Translation translation = translations.active(comment.getTranslationId());
        Rules.require("OPEN".equals(comment.getResolutionStatus()), "Comment is already resolved");
        Rules.require("REVISION".equals(translation.getStatus()), "Translation must be in revision");
        Rules.text(correctedText, "text");
        Rules.require(!correctedText.strip().equals(translation.getText().strip()), "Provide corrected translation text");
        translation.setText(correctedText); translation.setUpdatedAt(Instant.now());
        comment.setResolutionStatus("RESOLVED"); comment.setUpdatedAt(Instant.now());
        return comment;
    }

    // 4. Для каждого сегмента допускается только один одобренный перевод.
    public Translation approve(Long id) {
        Translation translation = translations.active(id);
        Rules.require("SUBMITTED".equals(translation.getStatus()), "Only a submitted translation can be approved");
        Rules.require(!hasOpenComments(id), "Translation has open comments");
        Rules.require(translationRepository.findBySegmentId(translation.getSegmentId()).stream()
                .noneMatch(t -> "APPROVED".equals(t.getStatus())), "Segment already has an approved translation");
        translation.setStatus("APPROVED"); translation.setUpdatedAt(Instant.now());
        return translation;
    }

    // 5. Завершить можно только непустой проект с одобренным переводом каждого сегмента.
    public TranslationProject complete(Long id) {
        TranslationProject project = projects.editable(id);
        Rules.require("IN_PROGRESS".equals(project.getStatus()), "Project must be in progress");
        List<SourceSegment> sourceSegments = segmentRepository.findByProjectId(id);
        Rules.require(!sourceSegments.isEmpty(), "Project has no segments");
        for (SourceSegment segment : sourceSegments) {
            List<Translation> variants = translationRepository.findBySegmentId(segment.getId());
            Rules.require(variants.stream().anyMatch(t -> "APPROVED".equals(t.getStatus())), "Every segment must have an approved translation");
            Rules.require(variants.stream().noneMatch(t -> hasOpenComments(t.getId())), "Project still has open comments");
        }
        project.setStatus("COMPLETED"); project.setUpdatedAt(Instant.now());
        return project;
    }

    private boolean hasOpenComments(Long translationId) {
        return commentRepository.findByTranslationId(translationId).stream()
                .anyMatch(c -> "OPEN".equals(c.getResolutionStatus()));
    }
}
