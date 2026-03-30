package com.envision.bunny.infrastructure.mybatis;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 需要Auditing的对象，可以继承此基类
 * @author jingjing.dong
 * @since 2021/4/15-18:03
 */
@Setter
@Getter
public abstract class AuditingEntity implements Serializable {
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    LocalDateTime createTime;
    /**
     * 创建人code
     */
    @TableField(fill = FieldFill.INSERT)
    String createBy;
    /**
     * 创建人姓名
     */
    @TableField(fill = FieldFill.INSERT)
    String createByName;
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    LocalDateTime updateTime;
    /**
     * 更新人code
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    String updateBy;

    /**
     * 更新人姓名
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    String updateByName;
}
