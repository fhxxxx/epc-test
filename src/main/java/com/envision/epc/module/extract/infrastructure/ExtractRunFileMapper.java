package com.envision.epc.module.extract.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.extract.domain.ExtractRunFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/9/4-19:44
 */
@Mapper
public interface ExtractRunFileMapper extends BaseMapper<ExtractRunFile> {
    @Select({
            """
            <script>
            SELECT
                ervf.extract_run_id as extractRunId,
                ervf.file_id as fileId,
                ervf.file_name as fileName,
                ervf.company_code as companyCode
            FROM t_extract_run_versions erv
            LEFT JOIN t_extract_run_version_files ervf
            ON erv.extract_run_id = ervf.extract_run_id and erv.version = ervf.version
            WHERE
                <choose>
                    <when test='runVersionIds != null and runVersionIds.size() > 0'>
                        erv.id in
                        <foreach collection="runVersionIds" item="item" open="(" separator="," close=")">
                            #{item}
                        </foreach>
                    </when>
                    <otherwise>
                        1 = 0
                    </otherwise>
                </choose>
            </script>
            """
    })
    List<ExtractRunFile> queryWithVersionIds(@Param("runVersionIds") List<Long> runVersionIds);

}
