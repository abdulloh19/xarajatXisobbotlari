-- V11: Enhance debts table and create debt_reminder_logs table

ALTER TABLE debts 
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'UZS',
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) DEFAULT 'CASH',
    ADD COLUMN IF NOT EXISTS borrowed_or_lent_date DATE DEFAULT CURRENT_DATE,
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS note TEXT;

ALTER TABLE debt_drafts
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) DEFAULT 'CASH',
    ADD COLUMN IF NOT EXISTS borrowed_or_lent_date DATE DEFAULT CURRENT_DATE;

CREATE TABLE IF NOT EXISTS debt_reminder_logs (
    id BIGSERIAL PRIMARY KEY,
    debt_id BIGINT NOT NULL REFERENCES debts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_type VARCHAR(50) NOT NULL,
    scheduled_date DATE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_debt_reminder_log UNIQUE (debt_id, reminder_type, scheduled_date)
);

CREATE INDEX IF NOT EXISTS idx_debt_reminder_logs_lookup ON debt_reminder_logs(debt_id, scheduled_date);
CREATE INDEX IF NOT EXISTS idx_debts_due_status ON debts(user_id, due_date, status);
