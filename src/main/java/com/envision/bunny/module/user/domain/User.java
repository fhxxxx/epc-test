package com.envision.bunny.module.user.domain;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.bunny.infrastructure.mybatis.AuditingEntity;
import com.envision.bunny.module.user.infrastructure.Constant;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.promeg.pinyinhelper.Pinyin;
import lombok.*;
import org.springframework.security.core.AuthenticatedPrincipal;

import java.io.Serializable;

/**
 * <p>
 * 系统用户表
 * </p>
 *
 * @author jingjing.dong
 * @since 2024-05-09
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "userCode", callSuper = false)
@TableName("sys_user")
@ToString
public class User extends AuditingEntity implements AuthenticatedPrincipal, Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * ID
     */
    @ExcelProperty(value = "ID",index = 0)
    @NumberFormat("#")
    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    protected Long userId;
    /**
     * 用户名
     *
     * @mock 董京京
     */
    @TableField("username")
    private String username;
    /**
     * 显示名称
     *
     * @mock 董京京(jingjing.dong)
     */
    @TableField("display_name")
    private String displayName;
    /**
     * 用户编号
     *
     * @mock 59930
     */
    @TableField("user_code")
    private String userCode;
    /**
     * 域账号
     *
     * @mock jingjing.dong
     */
    @TableField("account")
    private String account;
    /**
     * 头像|url地址
     *
     * @mock https://platform.envisioncn.com/apps/it/lightning/photo/59930_64.jpg
     */
    @TableField("avatar")
    private String avatar;
    /**
     * 员⼯号、域账号、拼⾳、汉字拼接⽽成
     *
     * @mock 59930jingjing.dongdongjingjing董京京
     */
    @TableField("search_str")
    private String searchStr;
    /**
     * 部门编号|DEP048
     *
     * @mock DEP048
     */
    @TableField("dept_code")
    private String deptCode;
    /**
     * 部门名称|智慧运营技术部
     *
     * @mock 智慧运营技术部
     */
    @TableField("dept_name")
    private String deptName;
    /**
     * 体系编号|DIV027
     *
     * @mock DIV027
     */
    @TableField("division_code")
    private String divisionCode;
    /**
     * 体系名称|智慧运营技术体系
     *
     * @mock 智慧运营技术体系
     */
    @TableField("division_name")
    private String divisionName;
    /**
     * 用户默认语言|zh_CN/en_US
     *
     * @mock zh_CN
     */
    @TableField("locale")
    private String locale;
    /**
     * 是否在职|0 代表否(离职)，非0值代表是(在职)
     *
     * @mock true
     */
    @TableField("is_in_service")
    private Boolean inService;

    public static User fromPltData(JsonNode attributes) {
        //1.从json中获取对应的值并赋值给相应字段
        String userCode = attributes.get("empno").asText("").trim();
        String username = attributes.get("empname").asText("").trim();
        String account = attributes.get("loginname").asText("").trim();
        String divisionCode = attributes.get("bunitno").asText("").trim();
        String divisionName = attributes.get("divname").asText("").trim();
        String deptNo = attributes.get("deptno").asText("").trim();
        String deptName = attributes.get("deptname").asText("").trim();
        String avatar = Constant.AVATAR_PREFIX + userCode + Constant.AVATAR_SUFFIX;
        String pinyin = Pinyin.toPinyin(username, "").toLowerCase();
        String hrStatus = attributes.get("hrstatus").asText("").trim();
        boolean inService = CharSequenceUtil.equals(hrStatus, "在职");
        final String country = attributes.get("f034").asText().trim();
        String locale = CharSequenceUtil.equalsAnyIgnoreCase(country,"null", "", "中国") ? "zh_CN" : "en_US";

        //2.构建SysUser对象
        User user = new User();
        if (inService) {
            user.setDisplayName(username + "(" + account + ")");
        } else {
            user.setDisplayName(username + "(" + account +"-已离职)");
        }
        user.setUsername(username);
        user.setUserCode(userCode);
        user.setAccount(account);
        user.setAvatar(avatar);
        user.setSearchStr(pinyin + account + userCode + username);
        user.setDeptCode(deptNo);
        user.setDeptName(deptName);
        user.setDivisionCode(divisionCode);
        user.setDivisionName(divisionName);
        user.setInService(inService);
        user.setLocale(locale);
        return user;
    }


    @Override
    public String getName() {
        return username;
    }
}
