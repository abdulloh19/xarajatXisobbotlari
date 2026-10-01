CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    type VARCHAR(20) NOT NULL, -- EXPENSE, INCOME
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'UZS',
    description TEXT,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL', -- MANUAL, TEXT, VOICE
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE transactions IS 'Financial transactions recorded by users';
