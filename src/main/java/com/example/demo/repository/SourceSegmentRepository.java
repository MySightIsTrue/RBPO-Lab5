package com.example.demo.repository;

import com.example.demo.model.SourceSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SourceSegmentRepository extends JpaRepository<SourceSegment, Long> {
    List<SourceSegment> findByProjectId(Long projectId);
}

