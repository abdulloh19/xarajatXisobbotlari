-- V15: Disable quiet hours by default so user-scheduled reminders are not delayed
ALTER TABLE notification_settings ALTER COLUMN quiet_hours_enabled SET DEFAULT FALSE;
UPDATE notification_settings SET quiet_hours_enabled = FALSE WHERE quiet_hours_enabled = TRUE;
