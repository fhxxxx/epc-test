package com.envision.bunny.module.extract.application.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * @author wenjun.gu
 * @since 2025/9/1-11:42
 */
@Getter
@Setter
@ToString
public class ExtractTaskDTO {
//    /**
//     * 文件id
//     */
//    private Long fileId;
    private Long id;
    /**
     * 顺序
     */
    private Integer position;
    /**
     * 开始页码
     */
    private Integer startPage;
    /**
     * 结束页码
     */
    private Integer endPage;

    private List<ExtractTaskResultDto> singleFieldResults;

    private Map<String, List<List<ExtractTaskResultDto>>> compositeFieldResults;
}
