INSERT INTO app_users (username, email, created_at) VALUES
('owner', 'owner@example.com', CURRENT_TIMESTAMP),
('translator', 'translator@example.com', CURRENT_TIMESTAMP);
INSERT INTO translation_projects (name, target_language, owner_id, status, created_at, updated_at)
VALUES ('Перевод инструкции', 'ru', 1, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO source_segments (project_id, source_text, segment_order, created_at) VALUES
(1, 'Hello, world!', 1, CURRENT_TIMESTAMP),
(1, 'Save the file.', 2, CURRENT_TIMESTAMP);
INSERT INTO translations (segment_id, author_id, text, status, created_at, updated_at) VALUES
(1, 2, 'Привет, мир!', 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 2, 'Сохраните файл.', 'REVISION', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO editorial_comments (translation_id, comment_text, resolution_status, created_at, updated_at)
VALUES (2, 'Уточните перевод слова file.', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
