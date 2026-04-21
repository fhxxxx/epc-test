package com.envision.epc.module.taxledger.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.taxledger.domain.FileParseStatusEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * TaxFileRecordMapper 数据访问接口
 */
@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {
    @Update("""
            <script>
            UPDATE t_file_record
            SET parse_status = #{targetStatus},
                parse_error_msg = NULL
            WHERE id = #{id}
              AND is_deleted = 0
              AND parse_status IN
              <foreach collection='allowStatuses' item='status' open='(' separator=',' close=')'>
                #{status}
              </foreach>
            </script>
            """)
    int transitionParseStatus(@Param("id") Long id,
                              @Param("targetStatus") FileParseStatusEnum targetStatus,
                              @Param("allowStatuses") FileParseStatusEnum[] allowStatuses);

    @Update("""
            UPDATE t_file_record
            SET parse_status = #{successStatus},
                parse_result_blob_path = #{parseResultBlobPath},
                parse_error_msg = NULL,
                parsed_at = #{parsedAt}
            WHERE id = #{id}
              AND is_deleted = 0
              AND parse_status = #{parsingStatus}
            """)
    int markParseSuccess(@Param("id") Long id,
                         @Param("successStatus") FileParseStatusEnum successStatus,
                         @Param("parsingStatus") FileParseStatusEnum parsingStatus,
                         @Param("parseResultBlobPath") String parseResultBlobPath,
                         @Param("parsedAt") LocalDateTime parsedAt);

    @Update("""
            UPDATE t_file_record
            SET parse_status = #{failedStatus},
                parse_error_msg = #{parseErrorMsg}
            WHERE id = #{id}
              AND is_deleted = 0
              AND parse_status = #{parsingStatus}
            """)
    int markParseFailed(@Param("id") Long id,
                        @Param("failedStatus") FileParseStatusEnum failedStatus,
                        @Param("parsingStatus") FileParseStatusEnum parsingStatus,
                        @Param("parseErrorMsg") String parseErrorMsg);
}

