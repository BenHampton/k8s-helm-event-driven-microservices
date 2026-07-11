--liquibase formatted sql

--changeset you:001-create-notifications
CREATE TABLE notifications (
    id             BIGSERIAL    PRIMARY KEY,
    order_id       BIGINT       NOT NULL UNIQUE,   -- one notification per order (idempotency)
    customer_email VARCHAR(320) NOT NULL,
    channel        VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    sent_at        TIMESTAMPTZ  NOT NULL
);
--rollback DROP TABLE notifications;