CREATE TABLE friends (
    id BIGSERIAL PRIMARY KEY,
    user1_id integer,
    user2_id integer,
    status varchar(10),
    created_at timestamp
);