package com.envision.epc.module.extract.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.envision.epc.module.extract.domain.ExtractRunStatusEnum;
import com.envision.epc.module.extract.domain.ExtractRunVersion;
import org.apache.ibatis.annotations.*;

/**
 * @author wenjun.gu
 * @since 2025/9/1-15:54
 */
@Mapper
public interface ExtractRunVersionMapper extends BaseMapper<ExtractRunVersion> {
    @Select({
            """
            <script>
            SELECT
                erv.id as id,
                erv.project_id as projectId,
                erv.extract_run_id as extractRunId,
                erv.version as version,
                erv.file_ids as fileIds,
                erv.extract_config as extractConfig,
                erv.status as status,
                erv.error as error,
                erv.ocr_result as ocrResult,
                erv.create_time as createTime,
                erv.create_by as createBy,
                erv.create_by_name as createByName,
                erv.update_time as updateTime,
                erv.update_by as updateBy,
                erv.update_by_name as updateByName
            FROM t_extract_runs er
            LEFT JOIN t_extract_run_versions erv
            ON er.id = erv.extract_run_id and er.current_version = erv.version
            WHERE er.project_id = #{projectId}
            <if test='status != null'>
                AND erv.status = #{status}
            </if>
            ORDER BY erv.create_time DESC
            </script>
            """
    })
    @Results({
            @Result(column = "extractConfig", property = "extractConfig", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class),
            @Result(column = "fileIds", property = "fileIds", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    })
    IPage<ExtractRunVersion> queryExtractRunWithStatus(IPage<ExtractRunVersion> page, @Param("projectId") Long projectId, @Param("status") ExtractRunStatusEnum status);
}
