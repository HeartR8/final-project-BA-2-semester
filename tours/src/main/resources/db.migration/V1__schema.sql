CREATE TABLE tickets (
    ticket_id VARCHAR(255) PRIMARY KEY,
    "from" VARCHAR(255) NOT NULL,
    "to" VARCHAR(255) NOT NULL,
    departure_time TIMESTAMPTZ NOT NULL,
    arrival_time TIMESTAMPTZ NOT NULL,
    price FLOAT NOT NULL
);

CREATE TABLE hotels (
                        hotel_id VARCHAR(255) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        description VARCHAR(255),
                        price_per_night FLOAT NOT NULL
);

CREATE TABLE tours (
    tour_id VARCHAR(255) PRIMARY KEY NOT NULL,
    hotel_id VARCHAR(255) REFERENCES hotels(hotel_id),
    ticket_to_id VARCHAR(255) REFERENCES tickets(ticket_id),
    ticket_from_id VARCHAR(255) REFERENCES tickets(ticket_id),
    is_booked BOOLEAN NOT NULL DEFAULT FALSE
)


