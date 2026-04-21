ALTER TABLE t_file_record
    ADD COLUMN parse_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态：PENDING/PARSING/SUCCESS/FAILED' AFTER file_size,
    ADD COLUMN parse_result_blob_path VARCHAR(500) NULL COMMENT '解析结果在Blob中的路径' AFTER parse_status,
    ADD COLUMN parse_error_msg VARCHAR(1000) NULL COMMENT '解析失败原因' AFTER parse_result_blob_path,
    ADD COLUMN parsed_at DATETIME NULL COMMENT '解析完成时间' AFTER parse_error_msg;

CREATE INDEX idx_file_parse_status ON t_file_record (is_deleted, parse_status, create_time);

