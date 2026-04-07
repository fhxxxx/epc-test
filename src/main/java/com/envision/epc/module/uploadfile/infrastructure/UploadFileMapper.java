package com.envision.epc.module.uploadfile.infrastructure;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.uploadfile.domain.UploadFile;
import org.apache.ibatis.annotations.Mapper;

/**
* @author wenjun.gu
* @since 2025/8/12-16:34
*/
@Mapper
public interface UploadFileMapper extends BaseMapper<UploadFile> {
}
