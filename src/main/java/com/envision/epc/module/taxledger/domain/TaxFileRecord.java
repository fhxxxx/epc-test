package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 税务文件记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_file_record")
public class TaxFileRecord extends AuditingEntity {
    /** 公司代码 */
    private String companyCode;
    /** 账期（yyyy-MM） */
    private String yearMonth;
    /** 文件名 */
    private String fileName;
    /** 文件类别 */
    private FileCategoryEnum fileCategory;
    /** 文件来源（上传/数据湖） */
    private FileSourceEnum fileSource;
    /** Blob存储路径 */
    private String blobPath;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 上传人工号 */
    private String uploadUser;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
