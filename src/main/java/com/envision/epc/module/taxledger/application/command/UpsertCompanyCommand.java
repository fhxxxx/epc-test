package com.envision.epc.module.taxledger.application.command;

import lombok.Data;

/**
 * 新增/更新公司命令
 */
@Data
public class UpsertCompanyCommand {
    /**
     * 公司主键（更新时使用）
     */
    private Long id;
    /**
     * 公司代码
     */
    private String companyCode;
    /**
     * 公司名称
     */
    private String companyName;
    /**
     * 财务BP AD账号
     */
    private String financeBpAd;
    /**
     * 财务BP姓名
     */
    private String financeBpName;
    /**
     * 财务BP邮箱
     */
    private String financeBpEmail;
    /**
     * 状态：1启用/0禁用
     */
    private Integer status;
}
