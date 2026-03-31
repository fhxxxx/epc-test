package com.envision.bunny.module.extract.application.dtos;

import com.envision.extract.module.extract.domain.Polygon;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author gangxiang.guan
 * @date 2025/9/23 14:35
 */
@Getter
@Setter
@ToString
public class ExtractTaskResultDto {
    private Long id;
    /**
     * t_extract_tasks表的关联id
     */
    private Long extractRunId;
    /**
     * t_extract_tasks表的关联id
     */
    private Long extractTaskId;
    /**
     * 组合名称(仅当字段类型为组合字段时有效)
     */
    private String compositeName;
    /**
     * 组合index(仅当字段类型为组合字段时有效)
     */
    private Long compositeIndex;
    /**
     * 字段名称
     */
    private String primitiveName;
    /**
     * 提取内容
     */
    private String content;
    /**
     * 位置信息json
     */
    private List<Polygon> polygons;
    /**
     * 段落范围
     */
    private String paraRange;
}
