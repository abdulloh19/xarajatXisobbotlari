-- V16: Set daily report default time to 23:59 and update existing settings
ALTER TABLE notification_settings ALTER COLUMN daily_report_time SET DEFAULT '23:59';
UPDATE notification_settings SET daily_report_time = '23:59' WHERE daily_report_time = '23:00';
