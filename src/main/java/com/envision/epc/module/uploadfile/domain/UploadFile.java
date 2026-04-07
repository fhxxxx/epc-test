package com.envision.epc.module.uploadfile.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_upload_files")
public class UploadFile extends AuditingEntity {
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 公司代码
     */
    private String companyCode;
    /**
     * 文件名称
     */
    private String name;
    /**
     * 文件页数
     */
    private Integer pages;
    /**
     * 文件大小
     */
    private Long size;
    /**
     * 文件路径(azure路径)
     */
    private String path;
    /**
     * 文件hash
     */
    private String hash;
    /**
     * 文件类型 INVOICE:用于提取的发票,DATALAKE：数据湖捞取后的文件,TAXBUREAU:用户上传的税务局文件, RESULT:发票提取的结果
     */
    private UploadTypeEnum type;

    public UploadFile(Long projectId, String companyCode, String name, Integer pages, Long size, String path, String hash,
                      UploadTypeEnum type, String createBy, String createByName, LocalDateTime createTime) {
        this.projectId = projectId;
        this.companyCode = companyCode;
        this.name = name;
        this.pages = pages;
        this.size = size;
        this.path = path;
        this.hash = hash;
        this.type = type;
        this.createBy = createBy;
        this.updateBy = createBy;
        this.createByName = createByName;
        this.updateByName = createByName;
        this.createTime = createTime;
        this.updateTime = createTime;
    }
}
