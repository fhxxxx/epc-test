package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运行产物（中间产物 / 最终台账）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ledger_run_artifact")
public class LedgerRunArtifact extends AuditingEntity {
    /** 运行 ID */
    private Long runId;

    /** 批次号 */
    private Integer batchNo;

    /** 产物类型 */
    private LedgerArtifactTypeEnum artifactType;

    /** 文件名 */
    private String fileName;

    /** Blob 路径 */
    private String blobPath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 校验值 */
    private String checksum;

    /** 是否当前最新：0-否，1-是 */
    private Integer isLatest;

    /** 逻辑删除标记：0-否，1-是 */
    private Integer isDeleted;
}
