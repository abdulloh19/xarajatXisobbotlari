CREATE TABLE IF NOT EXISTS transaction_drafts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    type VARCHAR(20) NOT NULL, -- EXPENSE, INCOME
    amount NUMERIC(19, 2),
    currency VARCHAR(10) NOT NULL DEFAULT 'UZS',
    description TEXT,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL', -- MANUAL, TEXT, VOICE
    original_text TEXT,
    confidence DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, CANCELLED, EXPIRED
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE transaction_drafts IS 'Temporary staging drafts before explicit user confirmation';
