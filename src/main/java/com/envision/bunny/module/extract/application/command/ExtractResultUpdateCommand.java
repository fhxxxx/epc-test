package com.envision.bunny.module.extract.application.command;

import com.envision.extract.module.validation.ValidProject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;

/**
 * @author gangxiang.guan
 * @date 2025/9/22 14:17
 */
@Getter
@Setter
@ToString
public class ExtractResultUpdateCommand {
    @ValidProject
    private Long projectId;
    //提取结果id
    @NotNull
    private Long extractResultId;
    @NotNull
    private Long extractRunId;
    //提取内容
    private String content;

}
