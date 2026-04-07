package com.envision.epc.module.extract.application.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/13-15:17
 */
@Getter
@Setter
@ToString
public class ExtractRunPageDTO {
    /**
     * id
     */
    private Long id;
    /**
     * 任务id
     */
    private Long extractRunId;
    /**
     * 运行版本id
     */
    private Long extractRunVersionId;
    /**
     * 项目id
     */
    private Long projectId;
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

    private List<File> fileList;

    private List<String> companyCodeList;
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

    @Data
    public static class File{
        private Long fileId;
        private String fileName;

        public File(Long fileId, String fileName) {
            this.fileId = fileId;
            this.fileName = fileName;
        }

        public File() {
        }
    }

}
