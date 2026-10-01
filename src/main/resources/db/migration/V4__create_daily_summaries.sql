CREATE TABLE IF NOT EXISTS daily_summaries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    summary_date DATE NOT NULL,
    total_income NUMERIC(19, 2) NOT NULL DEFAULT 0,
    total_expense NUMERIC(19, 2) NOT NULL DEFAULT 0,
    net_profit NUMERIC(19, 2) NOT NULL DEFAULT 0,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_daily_summaries_user_date UNIQUE (user_id, summary_date)
);

COMMENT ON TABLE daily_summaries IS 'Daily financial closure and summary per user';
