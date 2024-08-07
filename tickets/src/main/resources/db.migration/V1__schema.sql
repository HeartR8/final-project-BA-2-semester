CREATE TABLE tickets
(
    ticket_id      VARCHAR(255) PRIMARY KEY,
    "from"         VARCHAR(255) NOT NULL,
    "to"           VARCHAR(255) NOT NULL,
    departure_time TIMESTAMPTZ  NOT NULL,
    arrival_time   TIMESTAMPTZ  NOT NULL,
    price          FLOAT        NOT NULL
);

CREATE TABLE tickets_events
(
    event_id  VARCHAR(255) NOT NULL,
    ticket_id VARCHAR(255) NOT NULL,
    event     VARCHAR(255) NOT NULL,
    processed BOOLEAN      NOT NULL DEFAULT FALSE
);
