CREATE TABLE clothes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(50),
    brand VARCHAR(50),
    type VARCHAR(50),
    colour VARCHAR(50),
    size VARCHAR(50),
    image_url VARCHAR(500),
    created_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);