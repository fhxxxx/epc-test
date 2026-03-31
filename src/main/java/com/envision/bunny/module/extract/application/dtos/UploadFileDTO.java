package com.envision.bunny.module.extract.application.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author wenjun.gu
 * @since 2025/9/1-12:17
 */
@Getter
@Setter
@ToString
public class UploadFileDTO {
    /**
     * 文件id
     * mock 1
     */
    private Long id;
    /**
     * 文件名称
     * @mock test.pdf
     */
    private String name;
}
