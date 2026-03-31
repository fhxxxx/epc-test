package com.envision.bunny.module.extract.application.command;

import com.envision.extract.module.validation.ValidProject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author gangxiang.guan
 * @date 2025/9/22 14:17
 */
@Getter
@Setter
@ToString
public class ExtractResultInsertCommand {
    @ValidProject
    private Long projectId;
    @NotNull
    private Long extractRunId;
    @NotNull
    private Long extractTaskId;
    @NotEmpty
    private String compositeName;
    private List<Field> primitiveFields;

    @Data
    public static class Field{
        @NotEmpty
        private String primitiveName;
        //提取内容
        @NotEmpty
        private String content;
    }


}
