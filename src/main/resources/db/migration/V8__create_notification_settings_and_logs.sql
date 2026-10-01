-- V8: Create notification settings, reminder logs, and report delivery logs

CREATE TABLE IF NOT EXISTS notification_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    profit_reminder_18_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    profit_reminder_21_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    daily_report_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    weekly_report_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    two_week_report_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    three_week_report_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    monthly_report_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    daily_report_time VARCHAR(10) NOT NULL DEFAULT '23:00',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notification_settings_user UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS reminder_delivery_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_type VARCHAR(50) NOT NULL,
    reminder_date DATE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reminder_delivery UNIQUE (user_id, reminder_type, reminder_date)
);

CREATE TABLE IF NOT EXISTS report_delivery_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    report_type VARCHAR(50) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_report_delivery UNIQUE (user_id, report_type, period_start, period_end)
);

CREATE INDEX IF NOT EXISTS idx_reminder_log_lookup ON reminder_delivery_log(user_id, reminder_type, reminder_date);
CREATE INDEX IF NOT EXISTS idx_report_log_lookup ON report_delivery_log(user_id, report_type, period_start, period_end);
