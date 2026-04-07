package com.envision.epc.infrastructure.audit.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 用户操作记录表
 * </p>
 *
 * @author liang.liu7
 * @since 2025-03-04
 */
@Getter
@Setter
@TableName("audit")
public class Audit extends AuditingEntity {
    /**
     * 平台
     */
    @TableField("platform")
    private String platform;

    /**
     * 租户
     */
    @TableField("tenant")
    private String tenant;

    /**
     * 操作者
     */
    @TableField("operator")
    private String operator;

    /**
     * 业务id
     */
    @TableField("biz_no")
    private String bizNo;

    /**
     * 模块
     */
    @TableField("module")
    private String module;

    /**
     * 操作类型
     */
    @TableField("type")
    private String type;

    /**
     * 成功操作内容
     */
    @TableField("content")
    private String content;

    /**
     * 操作时间 时间戳单位：ms
     */
    @TableField("operate_time")
    private Long operateTime;

    /**
     * 操作花费的时间 单位：ms
     */
    @TableField("execute_time")
    private Long executeTime;

    /**
     * 是否调用成功
     */
    @TableField("success")
    private Boolean success;

    /**
     * 执行后返回的json字符串
     */
    @TableField("result")
    private String result;

    /**
     * 报错信息
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 详细
     */
    @TableField("details")
    private String details;
}
