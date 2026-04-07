package com.envision.epc.module.project.application.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/07-15:40
 */
@Setter
@Getter
@ToString
public class ProjectDTO {
    /**
     * 项目id
     * @mock 123
     */
    private Long id;
    /**
     * 项目名称
     * @mock 测试项目
     */
    private String name;
    /**
     * 创建时间
     * @mock 2025-08-21 00:00:00
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建人姓名
     */
    private String createByName;
}
