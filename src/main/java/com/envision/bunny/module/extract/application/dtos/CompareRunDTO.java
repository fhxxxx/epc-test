package com.envision.bunny.module.extract.application.dtos;

import com.envision.extract.module.extract.domain.CompareRunStatusEnum;
import com.envision.extract.module.extract.domain.CompareTypeEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/9/1-15:26
 */
@Data
public class CompareRunDTO {
    /**
     * id
     */
    private Long id;
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 类型
     */
    private CompareTypeEnum type;
    /**
     * upload_file表中提取结果文件id
     */
    private List<String> resultFileNames;
    /**
     * upload_file表中数据湖捞取结果文件id
     */
    private List<String> dataLakeFileNames;
    /**
     * upload_file表中数据湖对比结果文件id
     */
    private List<String> dataLakeCompareFileNames;
    /**
     * upload_file表中税务局文件id
     */
    private List<String> taxBureauFileNames;
    /**
     * 抽取状态
     */
    private CompareRunStatusEnum status;
    /**
     * 对比涉及的公司code
     */
    private List<String> companyCodeList;
    /**
     * 错误信息
     */
    private String error;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public LocalDateTime createTime;
    public String createBy;
    public String createByName;
}
