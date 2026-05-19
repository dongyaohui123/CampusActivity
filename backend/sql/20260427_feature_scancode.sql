ALTER TABLE activity_registrations
    ADD COLUMN ticket_code VARCHAR(64) NULL AFTER cancelled_at,
    ADD COLUMN ticket_issued_at DATETIME NULL AFTER ticket_code,
    ADD COLUMN checkin_operator_id BIGINT NULL AFTER checkin_at;

CREATE UNIQUE INDEX uk_activity_registrations_ticket_code
    ON activity_registrations (ticket_code);
