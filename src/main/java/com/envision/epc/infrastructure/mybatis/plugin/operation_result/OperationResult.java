package com.envision.epc.infrastructure.mybatis.plugin.operation_result;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
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
