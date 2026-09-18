CREATE TABLE outfit_clothes (
    outfit_id BIGINT,
    clothes_id BIGINT,
    PRIMARY KEY (outfit_id, clothes_id),
    FOREIGN KEY (outfit_id) REFERENCES outfit(id),
    FOREIGN KEY (clothes_id) REFERENCES clothes(id)
);