package com.envision.epc.module.extract.application.query;

import com.envision.epc.infrastructure.mybatis.BasicPaging;
import com.envision.epc.module.validation.ValidProject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.NotNull;

/**
 * @author wenjun.gu
 * @since 2025/8/13-15:19
 */
@Getter
@Setter
@ToString
public class CompareRunQuery extends BasicPaging {
    /**
     * 项目id
     *
     * @mock 123
     */
    @NotNull
    @ValidProject
    private Long projectId;
}
