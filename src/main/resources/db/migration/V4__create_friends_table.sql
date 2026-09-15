CREATE TABLE friends (
    id BIGSERIAL PRIMARY KEY,
    user1_id BIGINT,
    user2_id BIGINT,
    status varchar(10),
    created_at timestamp,
    FOREIGN KEY (user1_id) REFERENCES users(id),
    FOREIGN KEY (user2_id) REFERENCES users(id)
);
