package com.envision.bunny.module.extract.application.command;

import com.envision.extract.module.extract.domain.ExtractConfig;
import com.envision.extract.module.validation.ValidProject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.UniqueElements;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-19:29
 */
@Getter
@Setter
@ToString
public class ExtractCommand {
    /**
     * 项目id
     * @mock 123
     */
    @NotNull
    @ValidProject
    private Long projectId;
    /**
     * 提取记录id为空则创建新记录
     * @mock 123
     */
    private Long extractRunId;
    /**
     * 文档列表
     * @mock {1,2,3}
    */
    @NotEmpty
    @UniqueElements
    private List<Long> fileIds;

    @NotNull(message = "extractConfig.id不能为空")
    private ExtractConfig extractConfig;

    public String assembleRedisKey() {
        return projectId + "-" + extractRunId + "-" + fileIds + "-" + extractConfig.getId();
    }
}
