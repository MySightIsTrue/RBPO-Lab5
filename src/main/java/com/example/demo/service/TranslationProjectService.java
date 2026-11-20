package com.example.demo.service;

import com.example.demo.model.TranslationProject;
import com.example.demo.repository.TranslationProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class TranslationProjectService {
    private final TranslationProjectRepository projects;
    private final UserService users;
    public TranslationProjectService(TranslationProjectRepository projects, UserService users) {
        this.projects = projects; this.users = users;
    }
    public List<TranslationProject> findAll() { return projects.findAll(); }
    public TranslationProject findById(Long id) {
        return projects.findById(id).orElseThrow(() -> Rules.missing("Project", id));
    }
    public TranslationProject editable(Long id) {
        TranslationProject project = findById(id);
        Rules.require(!"COMPLETED".equals(project.getStatus()), "Completed project cannot be changed");
        return project;
    }
    public TranslationProject create(TranslationProject project) {
        Rules.text(project.getName(), "name");
        Rules.text(project.getTargetLanguage(), "targetLanguage");
        Rules.require(project.getOwnerId() != null, "ownerId is required");
        users.findById(project.getOwnerId());
        Rules.initialStatus(project.getStatus(), "DRAFT");
        project.setId(null); project.setStatus("DRAFT");
        project.setCreatedAt(Instant.now()); project.setUpdatedAt(project.getCreatedAt());
        return projects.save(project);
    }
    public TranslationProject update(Long id, TranslationProject patch) {
        TranslationProject project = editable(id);
        Rules.require(patch.getStatus() == null || patch.getStatus().equals(project.getStatus()), "Use business operations to change status");
        if (patch.getName() != null) { Rules.text(patch.getName(), "name"); project.setName(patch.getName()); }
        if (patch.getTargetLanguage() != null) {
            Rules.require("DRAFT".equals(project.getStatus()), "Language can only change in a draft project");
            Rules.text(patch.getTargetLanguage(), "targetLanguage"); project.setTargetLanguage(patch.getTargetLanguage());
        }
        if (patch.getOwnerId() != null) { users.findById(patch.getOwnerId()); project.setOwnerId(patch.getOwnerId()); }
        project.setUpdatedAt(Instant.now());
        return projects.save(project);
    }
    public void delete(Long id) {
        // Сегменты, переводы и замечания удаляются каскадно самой БД.
        projects.delete(editable(id));
    }
}
