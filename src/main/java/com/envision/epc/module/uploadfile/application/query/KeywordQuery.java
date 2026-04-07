package com.envision.epc.module.uploadfile.application.query;

import com.envision.epc.module.uploadfile.domain.UploadTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/14-14:41
 */
@Getter
@Setter
@ToString
public class KeywordQuery {
    private List<UploadTypeEnum> typeList;

    /**
     * 公司code
     */
    private String companyCode;
    /**
     * 关键字模糊搜索
     */
    private String keyword;
    /**
     * 分页大小
     * @mock 15
     */
    private Integer pageSize = 15;
    /**
     * 页标
     * @mock 1
     */
    private Integer pageNum = 1;
}
