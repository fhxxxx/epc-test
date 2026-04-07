package com.envision.epc.module.extract.domain;

/**
 * @author wenjun.gu
 * @since 2025/8/12-19:15
 */
public enum CompareTypeEnum {
    /**
     * 发票提取和数据湖数据对比
     */
    DATALAKECOMPARE,
    /**
     * 第一步对比结论与税务局文件对比
     */
    TAXBUREAUCOMPARE
}
