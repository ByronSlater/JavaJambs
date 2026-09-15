CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255),
    profile_picture VARCHAR(500),
    bio TEXT,
    created_at TIMESTAMP
);
