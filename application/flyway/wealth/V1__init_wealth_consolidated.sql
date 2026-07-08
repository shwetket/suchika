CREATE TABLE wealth.account (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id       UUID,
    institution_name VARCHAR(50)  NOT NULL,
    account_name     VARCHAR(50)  NOT NULL,
    account_type     VARCHAR(50)  NOT NULL,
    currency         VARCHAR(10)  NOT NULL DEFAULT 'INR',
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    opening_balance  NUMERIC(19,4),
    credit_limit     NUMERIC(19,4),
    interest_rate    NUMERIC(7,4),
    emi_amount       NUMERIC(19,4),
    metadata         JSONB        NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT fk_account_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);

CREATE TABLE wealth.statement_upload (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    account_id  UUID         NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    upload_date TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status      VARCHAR(20),
    CONSTRAINT pk_statement_upload PRIMARY KEY (id),
    CONSTRAINT fk_upload_account
        FOREIGN KEY (account_id) REFERENCES wealth.account(id)
);

CREATE TABLE wealth.upload_error_log (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    upload_id       UUID         NOT NULL,
    error_type      VARCHAR(50),
    missing_columns TEXT[],
    error_detail    TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_upload_error_log PRIMARY KEY (id)
);

CREATE TABLE wealth.transaction (
    id          UUID          NOT NULL DEFAULT gen_random_uuid(),
    account_id  UUID          NOT NULL,
    upload_id   UUID,
    txn_date    DATE          NOT NULL,
    amount      NUMERIC(19,4) NOT NULL,
    txn_type    VARCHAR(10)   NOT NULL,
    description TEXT          NOT NULL,
    metadata    JSONB         NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_transaction PRIMARY KEY (id),
    CONSTRAINT fk_txn_account
        FOREIGN KEY (account_id) REFERENCES wealth.account(id),
    CONSTRAINT fk_txn_upload
        FOREIGN KEY (upload_id) REFERENCES wealth.statement_upload(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_transaction_dedup
        UNIQUE (account_id, txn_date, amount, txn_type)
);

CREATE TABLE wealth.physical_asset (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    profile_id          UUID         NOT NULL,
    asset_name          VARCHAR(50)  NOT NULL,
    asset_type          VARCHAR(50)  NOT NULL,
    make                VARCHAR(100),
    model               VARCHAR(100),
    registration_number VARCHAR(50),
    registration_type   VARCHAR(50),
    metadata            JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_physical_asset PRIMARY KEY (id),
    CONSTRAINT uq_registration_number
        UNIQUE (registration_number),
    CONSTRAINT fk_physical_asset_profile
        FOREIGN KEY (profile_id)
        REFERENCES profile.profile(id)
        ON DELETE RESTRICT
);
