-- V17: Delete mistakenly entered daily profit for current date
DELETE FROM daily_profits WHERE profit_date = CURRENT_DATE;
