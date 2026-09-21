CREATE TABLE outfit_calendar (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    outfit_id BIGINT,
    planned_date DATE NOT NULL,
    note VARCHAR(280) NOT NULL,
    theme VARCHAR(20),
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (outfit_id) REFERENCES outfit(id)
);

CREATE INDEX idx_outfit_calendar_user_planned_date ON outfit_calendar(user_id, planned_date);
