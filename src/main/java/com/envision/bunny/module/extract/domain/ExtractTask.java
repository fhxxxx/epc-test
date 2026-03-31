package com.envision.bunny.module.extract.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.extract.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_extract_tasks", autoResultMap = true)
public class ExtractTask extends AuditingEntity {
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 抽取记录id
     */
    private Long extractRunId;
    /**
     * 文件id
     */
    private Long fileId;
    /**
     * 顺序
     */
    private Integer position;
    /**
     * 页码
     */
    private Integer startPage;
    private Integer endPage;

    /**
     * 版本
     */
    private Integer version;

    @TableField(exist = false)
    private List<ExtractTaskResult> extractTaskResults;

//    public void success(String result) {
////        this.status = ExtractTaskStatusEnum.SUCCESS;
//        this.result = result;
//    }
//
//    public void failed(String error) {
//        this.status = ExtractTaskStatusEnum.FAILED;
//        this.error = error;
//    }
}
