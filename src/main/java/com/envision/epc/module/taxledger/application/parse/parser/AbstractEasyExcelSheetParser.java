package com.envision.epc.module.taxledger.application.parse.parser;

/**
 * 简单固定表头场景的解析基类。
 * 当前复用Aspose读取实现，业务上按EasyExcel策略组织字段映射。
 */
public abstract class AbstractEasyExcelSheetParser<T> extends AbstractAsposeSheetParser<T> {
}
