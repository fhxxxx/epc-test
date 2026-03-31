package com.envision.bunny.module.permission.application.command;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:47
 */
@Setter
@Getter
@ToString
public class PermissionCommand {
    private List<String> userCode;
}
