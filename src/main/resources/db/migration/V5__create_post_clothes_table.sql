CREATE TABLE post_clothes (
    post_id BIGINT,
    clothes_id BIGINT,
    PRIMARY KEY (post_id, clothes_id),
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (clothes_id) REFERENCES clothes(id)
);
