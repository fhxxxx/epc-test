package com.envision.bunny.module.uploadfile.application;

import com.envision.extract.module.uploadfile.application.dtos.UploadFileDTO;
import com.envision.extract.module.uploadfile.domain.UploadFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @author wenjun.gu
 * @since 2025/8/15-11:59
 */
@Mapper(componentModel = "spring")
public abstract class UploadFileAssembler {

    @Mapping(target = "createBy", source = "uploadFile.createBy")
    @Mapping(target = "createByName", source = "uploadFile.createByName")
    public abstract UploadFileDTO toUploadFileDTO(UploadFile uploadFile);
}
