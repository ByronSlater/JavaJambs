CREATE TABLE outfit (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    outfit_name VARCHAR(50),
    type VARCHAR(50),
    image_url VARCHAR(500),
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);