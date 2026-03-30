package com.envision.bunny.infrastructure.mybatis.plugin.operation_result;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.bunny.infrastructure.mybatis.AuditingEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 数据更改记录表
 * </p>
 *
 * @author jingjing.dong
 * @since 2024-04-20
 */
@Getter
@Setter
@TableName("operation_result")
public class OperationResult extends AuditingEntity {
    /**
     * ID
     */
    @ExcelProperty(value = "ID",index = 0)
    @NumberFormat("#")
    @TableId(value = "operation_result_id", type = IdType.ASSIGN_ID)
    protected Long operationResultId;
    @TableField("operation")
    private String operation;

    @TableField("record_status")
    private Boolean recordStatus;

    @TableField("table_name")
    private String tableName;

    @TableField("changed_data")
    private String changedData;
    /**
     * 耗费时间(ms)
     */
    @TableField("cost")
    private long cost;
}
