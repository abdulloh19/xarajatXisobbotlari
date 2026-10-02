-- V12: Enhance debts table with original, paid, remaining amounts and create debt_payments and user_balances tables

-- 1. Enhance debts table
ALTER TABLE debts
    ADD COLUMN IF NOT EXISTS original_amount NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS remaining_amount NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS initial_payment_method VARCHAR(20),
    ADD COLUMN IF NOT EXISTS start_date DATE DEFAULT CURRENT_DATE;

-- Backfill existing debts
UPDATE debts SET original_amount = amount WHERE original_amount IS NULL;
UPDATE debts SET remaining_amount = CASE 
    WHEN status IN ('PAID', 'RECEIVED') THEN 0 
    ELSE amount 
END WHERE remaining_amount IS NULL;
UPDATE debts SET paid_amount = amount WHERE status IN ('PAID', 'RECEIVED');
UPDATE debts SET initial_payment_method = payment_method WHERE initial_payment_method IS NULL;
UPDATE debts SET start_date = borrowed_or_lent_date WHERE start_date IS NULL;

-- 2. Enhance debt_drafts table
ALTER TABLE debt_drafts
    ADD COLUMN IF NOT EXISTS initial_payment_method VARCHAR(20) DEFAULT 'Naqd';

-- 3. Create debt_payments table for payment history
CREATE TABLE IF NOT EXISTS debt_payments (
    id BIGSERIAL PRIMARY KEY,
    debt_id BIGINT NOT NULL REFERENCES debts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC(19, 2) NOT NULL,
    payment_type VARCHAR(30) NOT NULL, -- DEBT_PAYMENT, DEBT_RETURN
    payment_method VARCHAR(20) NOT NULL DEFAULT 'Naqd',
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_debt_payments_debt ON debt_payments(debt_id);
CREATE INDEX IF NOT EXISTS idx_debt_payments_user_date ON debt_payments(user_id, payment_date);

-- 4. Create user_balances table
CREATE TABLE IF NOT EXISTS user_balances (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    cash_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    card_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_balances_user_id ON user_balances(user_id);
