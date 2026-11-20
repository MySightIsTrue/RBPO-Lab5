package com.example.demo.service;

import com.example.demo.model.EditorialComment;
import com.example.demo.model.Translation;
import com.example.demo.repository.EditorialCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class EditorialCommentService {
    private final EditorialCommentRepository comments;
    private final TranslationService translations;
    public EditorialCommentService(EditorialCommentRepository comments, TranslationService translations) {
        this.comments = comments; this.translations = translations;
    }
    public List<EditorialComment> findAll() { return comments.findAll(); }
    public EditorialComment findById(Long id) { return comments.findById(id).orElseThrow(() -> Rules.missing("Comment", id)); }
    public EditorialComment create(EditorialComment comment) {
        Rules.require(comment.getTranslationId() != null, "translationId is required");
        Translation translation = translations.active(comment.getTranslationId());
        Rules.require("SUBMITTED".equals(translation.getStatus()) || "REVISION".equals(translation.getStatus()), "Only a submitted translation or revision can receive comments");
        Rules.text(comment.getCommentText(), "commentText");
        Rules.initialStatus(comment.getResolutionStatus(), "OPEN");
        comment.setId(null); comment.setResolutionStatus("OPEN");
        comment.setCreatedAt(Instant.now()); comment.setUpdatedAt(comment.getCreatedAt());
        translation.setStatus("REVISION"); translation.setUpdatedAt(Instant.now());
        return comments.save(comment);
    }
    public EditorialComment update(Long id, EditorialComment patch) {
        EditorialComment comment = findById(id);
        translations.active(comment.getTranslationId());
        Rules.require("OPEN".equals(comment.getResolutionStatus()), "Resolved comment cannot be edited");
        Rules.require(patch.getTranslationId() == null || patch.getTranslationId().equals(comment.getTranslationId()), "Cannot move a comment");
        Rules.require(patch.getResolutionStatus() == null || patch.getResolutionStatus().equals(comment.getResolutionStatus()), "Use resolve operation");
        if (patch.getCommentText() != null) { Rules.text(patch.getCommentText(), "commentText"); comment.setCommentText(patch.getCommentText()); }
        comment.setUpdatedAt(Instant.now());
        return comments.save(comment);
    }
    public void delete(Long id) {
        EditorialComment comment = findById(id);
        translations.active(comment.getTranslationId());
        Rules.require("RESOLVED".equals(comment.getResolutionStatus()), "Resolve the comment before deleting it");
        comments.delete(comment);
    }
}
