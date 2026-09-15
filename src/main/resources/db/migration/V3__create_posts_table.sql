CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    caption TEXT,
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);