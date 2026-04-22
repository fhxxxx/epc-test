SET @file_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 't_file_record'
);

SET @file_year_month_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 't_file_record'
      AND column_name = 'year_month'
);

SET @file_period_month_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 't_file_record'
      AND column_name = 'period_month'
);

SET @ddl := IF(
    @file_exists = 1 AND @file_year_month_exists = 1 AND @file_period_month_exists = 0,
    'ALTER TABLE t_file_record
        DROP INDEX uk_file_active,
        DROP INDEX idx_file_company_month,
        DROP COLUMN uk_file_year_month_active,
        CHANGE COLUMN `year_month` period_month VARCHAR(7) NOT NULL,
        ADD COLUMN uk_file_period_month_active VARCHAR(7) GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN period_month ELSE NULL END) VIRTUAL,
        ADD UNIQUE KEY uk_file_active (uk_file_company_code_active, uk_file_period_month_active, uk_file_category_active),
        ADD KEY idx_file_company_month (company_code, period_month)',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ledger_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 't_ledger_record'
);

SET @ledger_year_month_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 't_ledger_record'
      AND column_name = 'year_month'
);

SET @ledger_period_month_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 't_ledger_record'
      AND column_name = 'period_month'
);

SET @ddl := IF(
    @ledger_exists = 1 AND @ledger_year_month_exists = 1 AND @ledger_period_month_exists = 0,
    'ALTER TABLE t_ledger_record
        DROP INDEX uk_ledger_active,
        DROP INDEX idx_ledger_company_month,
        DROP COLUMN uk_ledger_year_month_active,
        CHANGE COLUMN `year_month` period_month VARCHAR(7) NOT NULL,
        ADD COLUMN uk_ledger_period_month_active VARCHAR(7) GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN period_month ELSE NULL END) VIRTUAL,
        ADD UNIQUE KEY uk_ledger_active (uk_ledger_company_code_active, uk_ledger_period_month_active),
        ADD KEY idx_ledger_company_month (company_code, period_month)',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
