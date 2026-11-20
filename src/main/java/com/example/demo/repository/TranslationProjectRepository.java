package com.example.demo.repository;

import com.example.demo.model.TranslationProject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TranslationProjectRepository extends JpaRepository<TranslationProject, Long> {
    List<TranslationProject> findByOwnerId(Long ownerId);
}

