--liquibase formatted sql

--changeset you:001-create-orders
CREATE TABLE orders (
id             BIGSERIAL PRIMARY KEY,
customer_email VARCHAR(320)  NOT NULL,
amount         NUMERIC(12,2) NOT NULL,
status         VARCHAR(20)   NOT NULL,
created_at     TIMESTAMPTZ   NOT NULL
);
--rollback DROP TABLE orders;
