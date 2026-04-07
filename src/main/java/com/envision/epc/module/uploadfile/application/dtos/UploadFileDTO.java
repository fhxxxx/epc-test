package com.envision.epc.module.uploadfile.application.dtos;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * @author wenjun.gu
 * @since 2025/8/14-14:37
 */
@Getter
@Setter
@ToString
public class UploadFileDTO {
    /**
     * 文件id
     * mock 1
     */
    private Long id;
    /**
     * 项目id
     *
     * @mock 1
     */
    private Long projectId;
    /**
     * 文件名称
     *
     * @mock test.pdf
     */
    private String name;
    /**
     * 公司code
     *
     * @mock 2023
     */
    private String companyCode;
    /**
     * 文件页数
     *
     * @mock 10
     */
    private Integer pages;
    /**
     * 文件大小
     *
     * @mock 100000
     */
    private Long size;
    /**
     * 文件hash
     *
     * @mock hash
     */
    private String hash;
    /**
     * 文件类型
     *
     * @mock hash
     */
    private String type;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    LocalDateTime createTime;
    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建人姓名
     */
    private String createByName;
}
