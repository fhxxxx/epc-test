package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.annotation.ExcelProperty;
import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.parse.EngineType;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.ParseSeverity;
import com.envision.epc.module.taxledger.application.parse.parser.AbstractEasyExcelSheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * BS附表解析器。
 * 对应sheet页：BS附表
 * 对应类别：FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE
 */
@Component
public class BsAppendixSheetParser extends AbstractEasyExcelSheetParser<List<BsAppendixUploadDTO>> {
    @Override
    protected Object openWorkbook(byte[] fileBytes, ParseContext context, ParseResult<List<BsAppendixUploadDTO>> result) {
        try {
            String sheetName = category().getTargetSheetName();
            if (sheetName != null && !sheetName.isBlank()) {
                return EasyExcelFactory.read(new ByteArrayInputStream(fileBytes))
                        .head(BsAppendixExcelRow.class)
                        .sheet(sheetName)
                        .doReadSync();
            }
            return EasyExcelFactory.read(new ByteArrayInputStream(fileBytes))
                    .head(BsAppendixExcelRow.class)
                    .sheet()
                    .doReadSync();
        } catch (Exception e) {
            result.addIssue(ParseSeverity.ERROR, "INVALID_WORKBOOK", e.getMessage());
            return null;
        }
    }

    @Override
    protected String locateSheet(Object workbook, ParseContext context, ParseResult<List<BsAppendixUploadDTO>> result) {
        return category().getTargetSheetName();
    }

    @Override
    protected HeaderData readHeader(Object workbook, String sheetName, ParseContext context, ParseResult<List<BsAppendixUploadDTO>> result) {
        return new HeaderData(List.of("公司", "总账科目", "短文本", "货币", "已结转余额", "累计余额"));
    }

    @Override
    protected BodyData readBody(Object workbook, String sheetName, ParseContext context, ParseResult<List<BsAppendixUploadDTO>> result) {
        @SuppressWarnings("unchecked")
        List<BsAppendixExcelRow> rows = (List<BsAppendixExcelRow>) workbook;
        List<List<String>> dataRows = new ArrayList<>();
        for (BsAppendixExcelRow row : rows) {
            dataRows.add(List.of(
                    emptyToBlank(row.getCompany()),
                    emptyToBlank(row.getGlAccount()),
                    emptyToBlank(row.getShortText()),
                    emptyToBlank(row.getCurrency()),
                    emptyToBlank(row.getCarriedForwardBalance()),
                    emptyToBlank(row.getCumulativeBalance())
            ));
        }
        return new BodyData(dataRows);
    }

    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE;
    }

    @Override
    public Class<List<BsAppendixUploadDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<BsAppendixUploadDTO>> cls = (Class<List<BsAppendixUploadDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    protected List<BsAppendixUploadDTO> mapToDto(HeaderData headerData,
                                                 BodyData bodyData,
                                                 ParseContext context,
                                                 ParseResult<List<BsAppendixUploadDTO>> result) {
        List<BsAppendixUploadDTO> rows = new ArrayList<>();
        for (List<String> row : bodyData.rows()) {
            String company = row.get(0);
            String glAccount = row.get(1);
            if ((company == null || company.isBlank()) && (glAccount == null || glAccount.isBlank())) {
                continue;
            }
            BsAppendixUploadDTO dto = new BsAppendixUploadDTO();
            dto.setCompanyCode(company);
            dto.setGlAccount(glAccount);
            dto.setShortText(row.get(2));
            dto.setCurrency(row.get(3));
            dto.setCarriedForwardBalance(row.get(4));
            dto.setCumulativeBalance(row.get(5));
            rows.add(dto);
        }
        return rows;
    }

    @Override
    protected EngineType engineType() {
        return EngineType.EASY_EXCEL;
    }

    private String emptyToBlank(String value) {
        return value == null ? "" : value;
    }

    @Data
    public static class BsAppendixExcelRow {
        @ExcelProperty("公司")
        private String company;

        @ExcelProperty("总账科目")
        private String glAccount;

        @ExcelProperty("短文本")
        private String shortText;

        @ExcelProperty("货币")
        private String currency;

        @ExcelProperty("已结转余额")
        private String carriedForwardBalance;

        @ExcelProperty("累计余额")
        private String cumulativeBalance;
    }
}
