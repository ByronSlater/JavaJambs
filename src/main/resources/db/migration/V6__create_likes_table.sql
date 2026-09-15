CREATE TABLE likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    post_id BIGINT,
    created_at timestamp,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (post_id) REFERENCES posts(id)
);