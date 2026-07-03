CREATE TABLE click_events (
    id              BIGSERIAL    PRIMARY KEY,
    short_code      VARCHAR(32)  NOT NULL,
    original_url    TEXT         NOT NULL,
    clicked_at      TIMESTAMP    NOT NULL DEFAULT now(),
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    referer         TEXT,
    country         VARCHAR(2)
);

CREATE INDEX idx_click_events_short_code ON click_events (short_code);
CREATE INDEX idx_click_events_clicked_at ON click_events (clicked_at);
