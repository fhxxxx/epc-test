package com.envision.epc.module.extract.application.command;

import com.envision.epc.module.validation.ValidProject;
import lombok.Data;
import lombok.ToString;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建发票提取和数据湖数据对比任务
 *
 * @author gangxiang.guan
 * @date 2025/10/11 15:22
 */
@Data
@ToString
public class DataLakeCompareCommand {
    /**
     * 项目id
     *
     * @mock 123
     */
    @NotNull
    @ValidProject
    private Long projectId;
    /**
     * 提取结果文件id
     *
     * @mock 123
     */
    @NotNull(message = "resultFileIds不能为空")
    private List<Long> resultFileIds;
    /**
     * 数据湖捞取结果文件id
     *
     * @mock 123
     */
    @NotNull(message = "dataLakeFileIds不能为空")
    private List<Long> dataLakeFileIds;

    public String assembleRedisKey() {
        return projectId + "-" + resultFileIds + "-" + dataLakeFileIds;
    }
}
