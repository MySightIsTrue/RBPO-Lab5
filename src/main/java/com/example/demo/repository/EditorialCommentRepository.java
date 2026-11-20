package com.example.demo.repository;

import com.example.demo.model.EditorialComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EditorialCommentRepository extends JpaRepository<EditorialComment, Long> {
    List<EditorialComment> findByTranslationId(Long translationId);
    void deleteByTranslationId(Long translationId);
}

