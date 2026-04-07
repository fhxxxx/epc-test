package com.envision.epc.module.extract.application.command;

import com.envision.epc.module.validation.ValidProject;
import lombok.Data;
import lombok.ToString;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建第一步对比结论与税务局文件对比任务
 *
 * @author gangxiang.guan
 * @date 2025/10/11 15:22
 */
@Data
@ToString
public class TaxBureauCompareCommand {
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
    @NotNull(message = "dataLakeCompareFileIds不能为空")
    private List<Long> dataLakeCompareFileIds;
    /**
     * 上传税务局文件id
     *
     * @mock 123
     */
    @NotNull(message = "taxBureauFileIds不能为空")
    private List<Long> taxBureauFileIds;

    public String assembleRedisKey() {
        return projectId + "-" + dataLakeCompareFileIds + "-" + taxBureauFileIds;
    }
}
