package com.envision.bunny.module.project.application.command;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/22-10:23
 */
@Setter
@Getter
@ToString
public class AlterProjectCommand {
    /**
     * 项目ID
     */
    private Long id;
    /**
     * 项目名称
     */
    private String name;
}
