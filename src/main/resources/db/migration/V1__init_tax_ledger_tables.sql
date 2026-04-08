CREATE TABLE IF NOT EXISTS t_tax_company (
    id BIGINT PRIMARY KEY,
    company_code VARCHAR(20) NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    finance_bp_ad VARCHAR(100) NULL,
    finance_bp_name VARCHAR(100) NULL,
    finance_bp_email VARCHAR(200) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tax_company_code (company_code)
);

CREATE TABLE IF NOT EXISTS t_tax_file_record (
    id BIGINT PRIMARY KEY,
    company_code VARCHAR(20) NOT NULL,
    `year_month` VARCHAR(7) NOT NULL,
    file_name VARCHAR(300) NOT NULL,
    file_category VARCHAR(60) NOT NULL,
    file_source VARCHAR(20) NOT NULL,
    blob_path VARCHAR(500) NOT NULL,
    file_size BIGINT NULL,
    upload_user VARCHAR(100) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tax_file (company_code, `year_month`, file_category, file_source),
    KEY idx_tax_file_company_month (company_code, `year_month`)
);

CREATE TABLE IF NOT EXISTS t_tax_ledger_record (
    id BIGINT PRIMARY KEY,
    company_code VARCHAR(20) NOT NULL,
    `year_month` VARCHAR(7) NOT NULL,
    ledger_name VARCHAR(300) NOT NULL,
    blob_path VARCHAR(500) NULL,
    generate_user VARCHAR(100) NULL,
    generate_status VARCHAR(20) NOT NULL,
    status_msg VARCHAR(500) NULL,
    generated_at DATETIME NULL,
    latest_run_id BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tax_ledger_company_month (company_code, `year_month`),
    KEY idx_tax_ledger_company_month (company_code, `year_month`)
);

CREATE TABLE IF NOT EXISTS t_tax_ledger_run (
    id BIGINT PRIMARY KEY,
    ledger_id BIGINT NOT NULL,
    run_no INT NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    mode_snapshot VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_batch INT NULL,
    input_fingerprint VARCHAR(128) NOT NULL,
    template_code VARCHAR(100) NULL,
    template_version VARCHAR(50) NULL,
    template_checksum VARCHAR(128) NULL,
    error_code VARCHAR(100) NULL,
    error_msg VARCHAR(1000) NULL,
    started_at DATETIME NULL,
    ended_at DATETIME NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tax_ledger_run (ledger_id, run_no),
    KEY idx_tax_ledger_run_status (status),
    KEY idx_tax_ledger_run_ledger (ledger_id)
);

CREATE TABLE IF NOT EXISTS t_tax_ledger_run_stage (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    batch_no INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    sheet_count_total INT NULL,
    sheet_count_success INT NULL,
    depends_on VARCHAR(100) NULL,
    error_msg VARCHAR(1000) NULL,
    confirm_user VARCHAR(100) NULL,
    confirm_time DATETIME NULL,
    started_at DATETIME NULL,
    ended_at DATETIME NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tax_run_stage (run_id, batch_no),
    KEY idx_tax_run_stage_status (status)
);

CREATE TABLE IF NOT EXISTS t_tax_ledger_run_artifact (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    batch_no INT NOT NULL,
    artifact_type VARCHAR(20) NOT NULL,
    artifact_name VARCHAR(300) NOT NULL,
    blob_path VARCHAR(500) NOT NULL,
    checksum VARCHAR(128) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_tax_run_artifact_run (run_id),
    KEY idx_tax_run_artifact_batch (run_id, batch_no)
);

CREATE TABLE IF NOT EXISTS t_tax_company_code_config (
    id BIGINT PRIMARY KEY,
    company_code VARCHAR(20) NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    finance_bp_ad VARCHAR(100) NULL,
    finance_bp_name VARCHAR(100) NULL,
    finance_bp_email VARCHAR(200) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tax_company_code_config (company_code)
);

CREATE TABLE IF NOT EXISTS t_tax_category_config (
    id BIGINT PRIMARY KEY,
    seq_no VARCHAR(20) NOT NULL,
    company_code VARCHAR(20) NULL,
    tax_type VARCHAR(50) NOT NULL,
    tax_category VARCHAR(50) NULL,
    tax_basis VARCHAR(200) NULL,
    collection_ratio DECIMAL(5, 2) NULL,
    tax_rate DECIMAL(10, 6) NULL,
    account_subject VARCHAR(200) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_tax_category_company_code (company_code)
);

CREATE TABLE IF NOT EXISTS t_tax_project_config (
    id BIGINT PRIMARY KEY,
    company_code VARCHAR(20) NOT NULL,
    tax_type VARCHAR(50) NOT NULL,
    tax_category VARCHAR(50) NULL,
    project_name VARCHAR(200) NULL,
    preferential_period VARCHAR(100) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_tax_project_company_code (company_code)
);

CREATE TABLE IF NOT EXISTS t_tax_vat_basic_item_config (
    id BIGINT PRIMARY KEY,
    item_seq INT NOT NULL,
    company_code VARCHAR(20) NULL,
    basic_item VARCHAR(200) NOT NULL,
    is_split VARCHAR(1) NOT NULL,
    is_display VARCHAR(1) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_tax_vat_basic_company_code (company_code)
);

CREATE TABLE IF NOT EXISTS t_tax_vat_special_item_config (
    id BIGINT PRIMARY KEY,
    item_seq INT NOT NULL,
    company_code VARCHAR(20) NOT NULL,
    special_item VARCHAR(200) NOT NULL,
    is_display VARCHAR(1) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_tax_vat_special_company_code (company_code)
);

CREATE TABLE IF NOT EXISTS t_tax_user_permission (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    employee_id VARCHAR(50) NOT NULL,
    permission_level VARCHAR(20) NOT NULL,
    company_code VARCHAR(20) NULL,
    granted_by VARCHAR(100) NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100) NULL,
    create_by_name VARCHAR(100) NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    update_by VARCHAR(100) NULL,
    update_by_name VARCHAR(100) NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tax_permission_user_company_level (user_id, company_code, permission_level),
    UNIQUE KEY uk_tax_permission_employee_company (employee_id, company_code),
    KEY idx_tax_permission_user (user_id),
    KEY idx_tax_permission_company (company_code)
);
