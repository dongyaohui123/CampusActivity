CREATE TABLE IF NOT EXISTS activity_manager_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    permissions VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_activity_manager_permissions_activity_user UNIQUE (activity_id, user_id)
);

CREATE INDEX idx_activity_manager_permissions_activity_status
    ON activity_manager_permissions (activity_id, status);
