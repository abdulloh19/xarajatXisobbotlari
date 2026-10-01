CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    username VARCHAR(255),
    language VARCHAR(10) NOT NULL DEFAULT 'uz',
    currency VARCHAR(10) NOT NULL DEFAULT 'UZS',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Tashkent',
    state VARCHAR(50) NOT NULL DEFAULT 'IDLE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE users IS 'Stores telegram bot users and their preferences';
