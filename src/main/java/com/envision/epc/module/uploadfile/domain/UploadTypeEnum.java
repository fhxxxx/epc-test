package com.envision.epc.module.uploadfile.domain;

import lombok.Getter;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-19:15
 */
@Getter
public enum UploadTypeEnum {
    /**
     * 用于提取的发票
     */
    INVOICE("INVOICE", "发票文件"),
    /**
     * 数据湖捞取后的文件
     */
    DATALAKE("INVOICE", "数据湖捞取文件"),
    /**
     * 用户上传的用于比较的税务局文件
     */
    TAXBUREAU("INVOICE", "税务局文件"),
    /**
     * 发票提取的结果
     */
    RESULT("INVOICE", "提取结果文件"),
    /**
     * 发票与数据湖对比结果文件
     */
    DATALAKECOMPARE("INVOICE", "数据湖对比结果文件");

    private final String code;
    private final String name;

    UploadTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static final List<UploadTypeEnum> EXCEL_FILE_LIST = List.of(DATALAKE, TAXBUREAU, RESULT, DATALAKECOMPARE);

}
