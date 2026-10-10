-- V14: Create To-Do Agent tables and add quiet hours / todo settings to notification_settings

-- 1. Enhance notification_settings with quiet hours and To-Do settings
ALTER TABLE notification_settings
    ADD COLUMN IF NOT EXISTS quiet_hours_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS quiet_hours_start VARCHAR(10) NOT NULL DEFAULT '23:00',
    ADD COLUMN IF NOT EXISTS quiet_hours_end VARCHAR(10) NOT NULL DEFAULT '07:00',
    ADD COLUMN IF NOT EXISTS todo_daily_brief_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS todo_daily_brief_time VARCHAR(10) NOT NULL DEFAULT '08:30',
    ADD COLUMN IF NOT EXISTS todo_weekly_review_enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Create todo_projects table
CREATE TABLE IF NOT EXISTS todo_projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    color VARCHAR(50) DEFAULT '📁',
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_todo_projects_user ON todo_projects(user_id, archived);

-- 3. Create todo_tasks table
CREATE TABLE IF NOT EXISTS todo_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id BIGINT REFERENCES todo_projects(id) ON DELETE SET NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    due_time TIME WITHOUT TIME ZONE,
    has_specific_time BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_at TIMESTAMP WITH TIME ZONE,
    recurrence_type VARCHAR(30) NOT NULL DEFAULT 'NONE',
    recurrence_days_of_week VARCHAR(100),
    recurrence_interval_days INTEGER,
    recurrence_day_of_month INTEGER,
    recurrence_end DATE,
    recurrence_parent_id BIGINT REFERENCES todo_tasks(id) ON DELETE SET NULL,
    planned_amount NUMERIC(19, 2),
    actual_amount NUMERIC(19, 2),
    currency VARCHAR(10) NOT NULL DEFAULT 'UZS',
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    linked_transaction_id BIGINT REFERENCES transactions(id) ON DELETE SET NULL,
    linked_debt_id BIGINT REFERENCES debts(id) ON DELETE SET NULL,
    snooze_count INTEGER NOT NULL DEFAULT 0,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_todo_tasks_user_status ON todo_tasks(user_id, status);
CREATE INDEX IF NOT EXISTS idx_todo_tasks_due_date ON todo_tasks(user_id, due_date);
CREATE INDEX IF NOT EXISTS idx_todo_tasks_reminder ON todo_tasks(reminder_at, status);
CREATE INDEX IF NOT EXISTS idx_todo_tasks_project ON todo_tasks(project_id, status);
CREATE INDEX IF NOT EXISTS idx_todo_tasks_recurrence_parent ON todo_tasks(recurrence_parent_id);

-- 4. Create todo_subtasks table
CREATE TABLE IF NOT EXISTS todo_subtasks (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES todo_tasks(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_todo_subtasks_task ON todo_subtasks(task_id, order_index);

-- 5. Create todo_reminder_logs table
CREATE TABLE IF NOT EXISTS todo_reminder_logs (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES todo_tasks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'DELIVERED',
    delivery_error TEXT
);

CREATE INDEX IF NOT EXISTS idx_todo_reminder_logs_user_date ON todo_reminder_logs(user_id, reminded_at);
CREATE INDEX IF NOT EXISTS idx_todo_reminder_logs_task ON todo_reminder_logs(task_id);
