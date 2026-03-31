package com.envision.bunny.module.extract.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.envision.extract.infrastructure.mybatis.AuditingEntity;
import lombok.*;

import java.util.List;

/**
 * <p>
 * 三方对比详情表
 * </p>
 *
 * @author jingjing.dong
 * @since 2025-12-03
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_compare_run_detail", autoResultMap = true)
public class CompareRunDetail extends AuditingEntity {

    /**
     * 关联t_compare_run表id
     */
    @TableField("compare_run_id")
    private Long compareRunId;

    /**
     * 公司代码
     */
    @TableField("company_code")
    private String companyCode;

    /**
     * upload_file表中提取结果文件id
     */
    @TableField(value = "result_file_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> resultFileIds;
    /**
     * 提取结果文件名称
     */
    @TableField(value = "result_file_names", typeHandler = JacksonTypeHandler.class)
    private List<String> resultFileNames;
    /**
     * upload_file表中数据湖捞取结果文件id
     */
    @TableField(value = "data_lake_file_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> dataLakeFileIds;
    /**
     * 数据湖捞取结果文件名称
     */
    @TableField(value = "data_lake_file_names", typeHandler = JacksonTypeHandler.class)
    private List<String> dataLakeFileNames;
    /**
     * upload_file表中数据湖对比结果文件id
     */
    @TableField(value = "data_lake_compare_file_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> dataLakeCompareFileIds;
    /**
     * 数据湖对比结果文件名称
     */
    @TableField(value = "data_lake_compare_file_names", typeHandler = JacksonTypeHandler.class)
    private List<String> dataLakeCompareFileNames;
    /**
     * upload_file表中税务局文件id
     */
    @TableField(value = "tax_bureau_file_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> taxBureauFileIds;
    /**
     * 税务局文件名称
     */
    @TableField(value = "tax_bureau_file_names", typeHandler = JacksonTypeHandler.class)
    private List<String> taxBureauFileNames;

    /**
     * 比对后结果存放路径
     */
    @TableField("compare_result")
    private String compareResult;
}
