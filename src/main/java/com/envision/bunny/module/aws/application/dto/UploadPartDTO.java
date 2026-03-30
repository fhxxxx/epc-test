package com.envision.bunny.module.aws.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadPartDTO {

    private Integer partNumber;

    private String eTag;

}