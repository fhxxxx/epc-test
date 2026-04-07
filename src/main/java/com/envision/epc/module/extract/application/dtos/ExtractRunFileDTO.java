package com.envision.epc.module.extract.application.dtos;

import com.envision.epc.module.extract.domain.ExtractRunStatusEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/9/9-17:49
 */
@Getter
@Setter
@ToString
public class ExtractRunFileDTO {
    /**
     * 文件id
     */
    private Long fileId;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 公司代码
     */
    private String companyCode;
    /**
     * 抽取状态
     */
    private ExtractRunStatusEnum status;
    /**
     * 错误信息
     */
    private String error;
    /**
     * 抽取结果
     */
    private List<ExtractTaskDTO> extractTasks = new ArrayList<>();
}
