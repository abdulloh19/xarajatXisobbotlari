-- V10: Create debts and debt_drafts tables for managing borrowed and lent debts

CREATE TABLE IF NOT EXISTS debts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL, -- BORROWED (qarz oldim), LENT (qarz berdim)
    person_name VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    due_date DATE,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, PAID, CANCELLED
    paid_at TIMESTAMP WITH TIME ZONE,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL', -- MANUAL, VOICE, TEXT
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS debt_drafts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    person_name VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    due_date DATE,
    description TEXT,
    raw_text TEXT,
    confidence DOUBLE PRECISION,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_debts_user_status ON debts(user_id, status);
CREATE INDEX IF NOT EXISTS idx_debts_user_due_date ON debts(user_id, due_date);
CREATE INDEX IF NOT EXISTS idx_debt_drafts_user_expiry ON debt_drafts(user_id, expires_at);
