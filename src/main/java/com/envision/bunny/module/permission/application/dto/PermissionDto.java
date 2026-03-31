package com.envision.bunny.module.permission.application.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:15
 */
@Setter
@Getter
@ToString
public class PermissionDto {
    /**
     * 权限id
     */
    private Long id;
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 用户工号
     */
    private String userCode;
    /**
     * 域账号
     */
    private String account;
    /**
     * 头像
     */
    private String avatar;
}
