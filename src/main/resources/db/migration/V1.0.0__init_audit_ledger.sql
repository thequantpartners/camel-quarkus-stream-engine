-- Audit Ledger Flyway Migration
CREATE TABLE IF NOT EXISTS audit_ledger (
    id UUID PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    account_id VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    destination_uri VARCHAR(255) NOT NULL,
    execution_details VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_idempotency ON audit_ledger (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_audit_event_id ON audit_ledger (event_id);
CREATE INDEX IF NOT EXISTS idx_audit_status ON audit_ledger (status);
