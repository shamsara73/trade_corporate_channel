--liquibase formatted sql

-- Transaction (TI master) and its events. Conventions: ADR 0003 (CHAR(36) UUID keys, names of at most
-- 30 characters, times in UTC from the application's clock, no vendor functions).

--changeset tcc:001-trade-txn
CREATE TABLE trade_txn (
    id                 CHAR(36)      NOT NULL,
    customer_mnemonic  VARCHAR(20)   NOT NULL,
    customer_name      NVARCHAR(1024),
    behalf_of_branch   VARCHAR(8),
    product_code       VARCHAR(4)    NOT NULL,
    e_bank_master_ref  VARCHAR(20)   NOT NULL,
    their_reference    VARCHAR(34)   NOT NULL,
    status             VARCHAR(16)   NOT NULL,
    currency_code      CHAR(3)       NOT NULL,
    finance_amount     DECIMAL(18,3) NOT NULL,
    outstanding_amount DECIMAL(18,3),
    start_date         DATE          NOT NULL,
    maturity_date      DATE          NOT NULL,
    ti_master_ref      VARCHAR(20),
    ti_status          VARCHAR(8),
    created_by         VARCHAR(64)   NOT NULL,
    created_at         DATETIME2     NOT NULL,
    updated_at         DATETIME2     NOT NULL,
    row_version        INT           NOT NULL,
    CONSTRAINT pk_trade_txn PRIMARY KEY (id),
    CONSTRAINT uq_trade_txn_ebank UNIQUE (e_bank_master_ref)
);
CREATE INDEX ix_trade_txn_customer ON trade_txn (customer_mnemonic, created_at);
--rollback DROP TABLE trade_txn;

--changeset tcc:001-trade-event
CREATE TABLE trade_event (
    id                 CHAR(36)      NOT NULL,
    txn_id             CHAR(36)      NOT NULL,
    event_type         VARCHAR(16)   NOT NULL,
    event_seq          INT           NOT NULL,
    e_bank_event_ref   VARCHAR(20)   NOT NULL,
    status             VARCHAR(16)   NOT NULL,
    ti_event_ref       VARCHAR(20),
    bank_comment       NVARCHAR(500),
    error_text         NVARCHAR(2000),
    created_by         VARCHAR(64)   NOT NULL,
    created_at         DATETIME2     NOT NULL,
    updated_at         DATETIME2     NOT NULL,
    completed_at       DATETIME2,
    row_version        INT           NOT NULL,
    CONSTRAINT pk_trade_event PRIMARY KEY (id),
    CONSTRAINT fk_trade_event_txn FOREIGN KEY (txn_id) REFERENCES trade_txn (id),
    CONSTRAINT uq_trade_event_ebank UNIQUE (e_bank_event_ref),
    CONSTRAINT uq_trade_event_seq UNIQUE (txn_id, event_seq)
);
--rollback DROP TABLE trade_event;

--changeset tcc:001-fsa-create-det
-- What the customer asked for in the CREATE event (TFFINNEW).
CREATE TABLE fsa_create_det (
    event_id           CHAR(36)      NOT NULL,
    their_reference    VARCHAR(34)   NOT NULL,
    currency_code      CHAR(3)       NOT NULL,
    finance_amount     DECIMAL(18,3) NOT NULL,
    txn_amount         DECIMAL(18,3) NOT NULL,
    start_date         DATE          NOT NULL,
    tenor_days         INT           NOT NULL,
    maturity_date      DATE          NOT NULL,
    trust_receipt      CHAR(1)       NOT NULL,
    interest_from      VARCHAR(16)   NOT NULL,
    product_type       VARCHAR(3)    NOT NULL,
    goods_desc         NVARCHAR(50),
    message_to_bank    NVARCHAR(2000),
    additional_info    NVARCHAR(222),
    CONSTRAINT pk_fsa_create_det PRIMARY KEY (event_id),
    CONSTRAINT fk_fsa_create_det_event FOREIGN KEY (event_id) REFERENCES trade_event (id)
);
--rollback DROP TABLE fsa_create_det;

--changeset tcc:001-fsa-repay-det
-- What the customer asked for in a REPAY event (TFFSARPY).
CREATE TABLE fsa_repay_det (
    event_id           CHAR(36)      NOT NULL,
    repayment_ref      VARCHAR(34)   NOT NULL,
    repayment_action   VARCHAR(32)   NOT NULL,
    value_date         DATE          NOT NULL,
    repayment_payer    VARCHAR(32)   NOT NULL,
    currency_code      CHAR(3)       NOT NULL,
    principal_amount   DECIMAL(18,3) NOT NULL,
    interest_amount    DECIMAL(18,3) NOT NULL,
    total_amount       DECIMAL(18,3) NOT NULL,
    CONSTRAINT pk_fsa_repay_det PRIMARY KEY (event_id),
    CONSTRAINT fk_fsa_repay_det_event FOREIGN KEY (event_id) REFERENCES trade_event (id)
);
--rollback DROP TABLE fsa_repay_det;

--changeset tcc:001-outbox-msg
-- Messages to TI, written in the same database transaction as their event (ground rule 7) and sent by
-- the outbox dispatcher. their_ref is the message reference: unique per delivery, also the CorrelationId.
CREATE TABLE outbox_msg (
    id                 CHAR(36)      NOT NULL,
    event_id           CHAR(36)      NOT NULL,
    their_ref          VARCHAR(34)   NOT NULL,
    operation          VARCHAR(16)   NOT NULL,
    status             VARCHAR(12)   NOT NULL,
    attempts           INT           NOT NULL,
    next_attempt_at    DATETIME2     NOT NULL,
    claimed_at         DATETIME2,
    sent_at            DATETIME2,
    destination        VARCHAR(512),
    body               ${long_text},
    last_error         NVARCHAR(2000),
    reply_status       VARCHAR(12),
    reply_details      NVARCHAR(2000),
    replied_at         DATETIME2,
    created_at         DATETIME2     NOT NULL,
    row_version        INT           NOT NULL,
    CONSTRAINT pk_outbox_msg PRIMARY KEY (id),
    CONSTRAINT fk_outbox_msg_event FOREIGN KEY (event_id) REFERENCES trade_event (id),
    CONSTRAINT uq_outbox_msg_their_ref UNIQUE (their_ref)
);
CREATE INDEX ix_outbox_msg_due ON outbox_msg (status, next_attempt_at);
--rollback DROP TABLE outbox_msg;

--changeset tcc:001-inbound-msg
-- Every message read from TI, matched or not, kept as received for audit and diagnosis.
CREATE TABLE inbound_msg (
    id                 CHAR(36)      NOT NULL,
    received_at        DATETIME2     NOT NULL,
    source             VARCHAR(120)  NOT NULL,
    kind               VARCHAR(12)   NOT NULL,
    operation          VARCHAR(32),
    message_name       VARCHAR(32),
    correlation_id     VARCHAR(48),
    e_bank_master_ref  VARCHAR(20),
    e_bank_event_ref   VARCHAR(20),
    event_id           CHAR(36),
    outcome            VARCHAR(16)   NOT NULL,
    body               ${long_text}  NOT NULL,
    CONSTRAINT pk_inbound_msg PRIMARY KEY (id),
    CONSTRAINT fk_inbound_msg_event FOREIGN KEY (event_id) REFERENCES trade_event (id)
);
CREATE INDEX ix_inbound_msg_refs ON inbound_msg (e_bank_master_ref, e_bank_event_ref);
--rollback DROP TABLE inbound_msg;

--changeset tcc:001-ti-document
-- Documents TI attaches to its outward messages, for example the debit/credit advice of a finance.
CREATE TABLE ti_document (
    id                 CHAR(36)      NOT NULL,
    event_id           CHAR(36)      NOT NULL,
    document_id        VARCHAR(34),
    description        NVARCHAR(200),
    mime_type          VARCHAR(100)  NOT NULL,
    content            ${long_binary} NOT NULL,
    document_ref       VARCHAR(100),
    received_at        DATETIME2     NOT NULL,
    CONSTRAINT pk_ti_document PRIMARY KEY (id),
    CONSTRAINT fk_ti_document_event FOREIGN KEY (event_id) REFERENCES trade_event (id)
);
--rollback DROP TABLE ti_document;
