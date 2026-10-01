-- V9: Create daily_profits table for recording cash and card profit in hand

CREATE TABLE IF NOT EXISTS daily_profits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    profit_date DATE NOT NULL,
    cash_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    card_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total_profit NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_daily_profits_user_date UNIQUE (user_id, profit_date)
);

CREATE INDEX IF NOT EXISTS idx_daily_profits_user_date ON daily_profits(user_id, profit_date);
