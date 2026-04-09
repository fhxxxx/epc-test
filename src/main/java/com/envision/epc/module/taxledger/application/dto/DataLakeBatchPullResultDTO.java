package com.envision.epc.module.taxledger.application.dto;

import com.envision.epc.module.taxledger.domain.FileRecord;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据湖批量拉取结果
 */
@Data
public class DataLakeBatchPullResultDTO {
    /**
     * 成功写入的文件记录（所有公司汇总）
     */
    private List<FileRecord> records = new ArrayList<>();

    /**
     * 失败明细（按公司）
     */
    private List<String> errors = new ArrayList<>();

    /**
     * 成功公司数
     */
    private Integer successCount;

    /**
     * 失败公司数
     */
    private Integer failCount;
}