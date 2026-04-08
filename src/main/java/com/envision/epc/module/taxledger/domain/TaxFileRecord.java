package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_file_record")
public class TaxFileRecord extends AuditingEntity {
    private String companyCode;
    private String yearMonth;
    private String fileName;
    private FileCategoryEnum fileCategory;
    private FileSourceEnum fileSource;
    private String blobPath;
    private Long fileSize;
    private String uploadUser;
    private Integer isDeleted;
}
