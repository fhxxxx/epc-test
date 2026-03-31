package com.envision.bunny.module.extract.application.query;

import com.envision.extract.infrastructure.mybatis.BasicPaging;
import com.envision.extract.module.extract.domain.ExtractRunStatusEnum;
import com.envision.extract.module.validation.ValidProject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;

/**
 * @author wenjun.gu
 * @since 2025/8/13-15:19
 */
@Getter
@Setter
@ToString
public class ExtractKeywordQuery extends BasicPaging {
    /**
     * 项目id
     * @mock 123
     */
    @NotNull
    @ValidProject
    private Long projectId;
    /**
     * 抽取状态
     * @mock SUCCESS
     */
    private ExtractRunStatusEnum status;
}
