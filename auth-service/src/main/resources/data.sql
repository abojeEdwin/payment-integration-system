CREATE TABLE merchants
(
    id               UUID                        NOT NULL,
    name             VARCHAR(255)                NOT NULL,
    email            VARCHAR(255)                NOT NULL,
    password_hash    VARCHAR(255)                NOT NULL,
    payment_provider VARCHAR(255)                NOT NULL,
    active           BOOLEAN                     NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_merchants PRIMARY KEY (id)
);

ALTER TABLE merchants
    ADD CONSTRAINT uc_b48071ed3ab5e33eedeedcfe8 UNIQUE (email);


CREATE TABLE api_keys
(
    id           UUID                        NOT NULL,
    merchant_id  UUID                        NOT NULL,
    key_value    VARCHAR(255)                NOT NULL,
    key_prefix   VARCHAR(255)                NOT NULL,
    description  VARCHAR(255)                NOT NULL,
    active       BOOLEAN                     NOT NULL,
    expires_at   TIMESTAMP WITHOUT TIME ZONE,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_api_keys PRIMARY KEY (id)
);

ALTER TABLE api_keys
    ADD CONSTRAINT uc_api_keys_key_value UNIQUE (key_value);

ALTER TABLE api_keys
    ADD CONSTRAINT FK_API_KEYS_ON_MERCHANT FOREIGN KEY (merchant_id) REFERENCES merchants (id);