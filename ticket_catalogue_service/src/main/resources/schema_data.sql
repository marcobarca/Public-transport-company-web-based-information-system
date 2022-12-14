DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS tickets CASCADE;
CREATE TABLE IF NOT EXISTS tickets
(
    id          BIGSERIAL,
    type_       VARCHAR(255),
    validity_zones  VARCHAR(5),
    price       FLOAT,
    min_age     INTEGER,
    max_age     INTEGER,
    CONSTRAINT pk_tickets_id
    PRIMARY KEY(id)
);
CREATE TABLE IF NOT EXISTS orders
(
    id          BIGSERIAL,
    ticket_id   INTEGER,
    quantity    INTEGER,
    status      VARCHAR(255),
    username    VARCHAR(255),
    purchased   BOOL,
    date_time  TIMESTAMP,
    CONSTRAINT pk_orders_id
    PRIMARY KEY(id),
    CONSTRAINT fk_orders_tickets_ticket_id
    FOREIGN KEY(ticket_id) REFERENCES tickets(id)
);

INSERT INTO tickets(type_, validity_zones, price, min_age, max_age)
VALUES('ORDINAL', 'ABC', 2.50, null, 30);
INSERT INTO tickets(type_, validity_zones, price, min_age, max_age)
VALUES('WEEKEND', 'CDE', 2.00, 18, null);
INSERT INTO tickets(type_, validity_zones, price, min_age, max_age)
VALUES('WEEKEND', 'B', 2.00, 18, 30);
INSERT INTO orders(ticket_id, quantity, status, username, purchased, date_time)
VALUES(1, 100, 'PENDING', 'john', false, CURRENT_TIMESTAMP);
INSERT INTO orders(ticket_id, quantity, status, username, purchased, date_time)
VALUES(2, 50, 'COMPLETED', 'jimmy', true, '2022-06-22 19:10:25-07');
INSERT INTO orders(ticket_id, quantity, status, username, purchased, date_time)
VALUES(3, 10, 'COMPLETED', 'john', false, '2022-06-15 00:00:00-01');
