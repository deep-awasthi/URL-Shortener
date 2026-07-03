CREATE TABLE urls (
    id              VARCHAR(26)  PRIMARY KEY,
    original_url    TEXT         NOT NULL,
    short_code      VARCHAR(32)  NOT NULL UNIQUE,
    creation_date   TIMESTAMP    NOT NULL DEFAULT now(),
    expiration_date TIMESTAMP    NOT NULL,
    click_count     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_urls_short_code ON urls (short_code);
CREATE INDEX idx_urls_expiration ON urls (expiration_date);
