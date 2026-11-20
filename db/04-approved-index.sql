-- PostgreSQL дополнительно защищает от двух одобренных вариантов одного сегмента.
CREATE UNIQUE INDEX uq_approved_translation_per_segment
    ON translations(segment_id) WHERE status = 'APPROVED';
