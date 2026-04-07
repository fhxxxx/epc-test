package com.envision.epc.module.user.application.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/12-15:25
 */
@Setter
@Getter
@ToString
public class UserDto {
    /**
     * 用户名
     *
     * @mock 董京京
     */
    private String username;
    /**
     * 用户编号
     *
     * @mock 59930
     */
    private String userCode;
    /**
     * 域账号
     *
     * @mock jingjing.dong
     */
    private String account;
    /**
     * 头像|url地址
     *
     * @mock https://platform.envisioncn.com/apps/it/lightning/photo/59930_64.jpg
     */
    private String avatar;
    /**
     * 员⼯号、域账号、拼⾳、汉字拼接⽽成
     *
     * @mock 59930jingjing.dongdongjingjing董京京
     */
    private String searchStr;
    /**
     * 部门编号|DEP048
     *
     * @mock DEP048
     */
    private String deptCode;
    /**
     * 部门名称|智慧运营技术部
     *
     * @mock 智慧运营技术部
     */
    private String deptName;
    /**
     * 体系编号|DIV027
     *
     * @mock DIV027
     */
    private String divisionCode;
    /**
     * 体系名称|智慧运营技术体系
     *
     * @mock 智慧运营技术体系
     */
    private String divisionName;
}
