-- Indexes for transaction reporting and user segregation
CREATE INDEX IF NOT EXISTS idx_transactions_user_date ON transactions (user_id, transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_user_type_date ON transactions (user_id, type, transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_user_cat_date ON transactions (user_id, category_id, transaction_date);

-- Categories indexes
CREATE INDEX IF NOT EXISTS idx_categories_user_type ON categories (user_id, type, is_active);

-- Drafts indexes
CREATE INDEX IF NOT EXISTS idx_drafts_user_status ON transaction_drafts (user_id, status);
CREATE INDEX IF NOT EXISTS idx_drafts_expires_at ON transaction_drafts (expires_at);

-- Daily summaries indexes
CREATE INDEX IF NOT EXISTS idx_summaries_user_date ON daily_summaries (user_id, summary_date);
