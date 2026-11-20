package com.example.demo.security;

import com.example.demo.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("access")
public class Access {
    private final UserRepository users;
    private final TranslationProjectRepository projects;
    private final SourceSegmentRepository segments;
    private final TranslationRepository translations;
    private final EditorialCommentRepository comments;
    public Access(UserRepository users, TranslationProjectRepository projects, SourceSegmentRepository segments,
                  TranslationRepository translations, EditorialCommentRepository comments) {
        this.users = users; this.projects = projects; this.segments = segments;
        this.translations = translations; this.comments = comments;
    }
    public boolean editor() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EDITOR"));
    }
    public boolean self(Long id) {
        return id != null && users.findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .map(u -> id.equals(u.getId())).orElse(false);
    }
    public boolean user(Long id) { return editor() || self(id); }
    public boolean project(Long id) {
        return editor() && id != null && projects.findById(id).map(p -> self(p.getOwnerId())).orElse(false);
    }
    public boolean segment(Long id) {
        return id != null && segments.findById(id).map(s -> project(s.getProjectId())).orElse(false);
    }
    public boolean author(Long id) {
        return id != null && translations.findById(id).map(t -> self(t.getAuthorId())).orElse(false);
    }
    public boolean review(Long id) {
        return editor() && id != null && translations.findById(id).map(t -> !self(t.getAuthorId())).orElse(false);
    }
    public boolean commentAuthor(Long id) {
        return id != null && comments.findById(id).map(c -> author(c.getTranslationId())).orElse(false);
    }
    public boolean commentReview(Long id) {
        return id != null && comments.findById(id).map(c -> review(c.getTranslationId())).orElse(false);
    }
}
