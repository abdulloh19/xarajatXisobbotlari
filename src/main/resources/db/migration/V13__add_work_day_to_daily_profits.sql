-- V13: Add is_work_day column to daily_profits table
ALTER TABLE daily_profits ADD COLUMN IF NOT EXISTS is_work_day BOOLEAN NOT NULL DEFAULT TRUE;
