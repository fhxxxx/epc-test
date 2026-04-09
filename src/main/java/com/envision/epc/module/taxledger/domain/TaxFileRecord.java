package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 税务文件记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_file_record")
public class TaxFileRecord extends AuditingEntity {
    /** 公司代码 */
    private String companyCode;

    /** 账期（yyyy-MM） */
    @TableField("`year_month`")
    private String yearMonth;

    /** 文件名 */
    private String fileName;

    /** 文件类别 */
    private FileCategoryEnum fileCategory;

    /** Blob 存储路径 */
    private String blobPath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 逻辑删除标记：0-否，1-是 */
    private Integer isDeleted;
}
