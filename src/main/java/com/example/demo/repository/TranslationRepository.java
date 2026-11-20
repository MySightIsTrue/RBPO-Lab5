package com.example.demo.repository;

import com.example.demo.model.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TranslationRepository extends JpaRepository<Translation, Long> {
    List<Translation> findBySegmentId(Long segmentId);
    List<Translation> findByAuthorId(Long authorId);
}

