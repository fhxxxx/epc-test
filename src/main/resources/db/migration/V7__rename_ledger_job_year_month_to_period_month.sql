SET @ledger_job_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 't_ledger_job'
);

SET @year_month_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 't_ledger_job'
      AND column_name = 'year_month'
);

SET @period_month_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 't_ledger_job'
      AND column_name = 'period_month'
);

SET @ddl := IF(
    @ledger_job_exists = 1 AND @year_month_exists = 1 AND @period_month_exists = 0,
    'ALTER TABLE t_ledger_job CHANGE COLUMN `year_month` period_month VARCHAR(7) NOT NULL',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
