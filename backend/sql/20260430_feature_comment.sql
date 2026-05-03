CREATE TABLE IF NOT EXISTS activity_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_comments_activity_created
    ON activity_comments (activity_id, created_at, id);

CREATE INDEX idx_activity_comments_user_id
    ON activity_comments (user_id);
