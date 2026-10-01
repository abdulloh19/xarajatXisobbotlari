CREATE TABLE IF NOT EXISTS processed_updates (
    id BIGSERIAL PRIMARY KEY,
    update_id BIGINT UNIQUE,
    callback_query_id VARCHAR(100) UNIQUE,
    action_key VARCHAR(255),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE processed_updates IS 'Idempotency registry to eliminate duplicate Telegram updates and double clicks';
