package com.envision.bunny.module.extract.application.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @author wenjun.gu
 * @since 2025/8/13-15:01
 */
@Getter
@Setter
@ToString
public class ExtractRunVersionDTO {
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 任务id
     */
    private Long id;
    /**
     * 任务id
     */
    private Long extractRunId;
    /**
     * 任务版本id
     */
    private Long extractRunVersionId;
    /**
     * 提取配置
     */
    private ExtractConfigDTO extractConfig;
    /**
     * 抽取状态
     */
    private String status;
    /**
     * 错误信息
     */
    private String error;
    /**
     * 版本
     */
    private Integer version;
    /**
     * ocr结果
     */
    private String ocrResult;
    /**
     * 抽取结果
     */
    private List<ExtractRunFileDTO> extractRunFiles;
    /**
     * 本次提取涉及公司列表
     */
    private Set<String> companyCodeList;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    /**
     * 创建人
     */
    private String createBy;
    /**
     * 创建人姓名
     */
    private String createByName;
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
    /**
     * 更新人
     */
    private String updateBy;
    /**
     * 更新人姓名
     */
    private String updateByName;

}
