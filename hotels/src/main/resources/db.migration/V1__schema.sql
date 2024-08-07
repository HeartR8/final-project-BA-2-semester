CREATE TABLE hotels (
    hotel_id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    price_per_night FLOAT NOT NULL
);

CREATE TABLE hotels_events (
    event_id VARCHAR(255) NOT NULL,
    hotel_id VARCHAR(255) NOT NULL,
    event VARCHAR(255) NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE
);
