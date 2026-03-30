package com.envision.bunny.module.aws.application.dto;

import lombok.Data;

/**
 * 分片信息
 */
@Data
public class PartDTO {

    private Integer partNumber;
    private String eTag;
}