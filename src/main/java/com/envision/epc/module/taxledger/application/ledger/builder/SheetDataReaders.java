package com.envision.epc.module.taxledger.application.ledger.builder;

import com.aspose.cells.Workbook;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * SheetData 构建读取工具。
 */
public final class SheetDataReaders {
    private SheetDataReaders() {
    }

    public static <E> List<E> requireList(LedgerBuildContext ctx,
                                          FileCategoryEnum category,
                                          Class<E> elementType,
                                          LedgerSheetCode sheetCode) {
        ensureFilePresent(ctx, category, sheetCode);
        List<E> rows;
        if (ctx.hasParsed(category)) {
            rows = ctx.getParsedList(category, elementType);
        } else {
            rows = ctx.getParsedDataGateway().readParsedList(category, elementType);
        }
        if (rows == null) {
            throw emptyParsed(category, sheetCode);
        }
        return rows;
    }

    public static <E> E requireObject(LedgerBuildContext ctx,
                                      FileCategoryEnum category,
                                      Class<E> type,
                                      LedgerSheetCode sheetCode) {
        ensureFilePresent(ctx, category, sheetCode);
        E value;
        if (ctx.hasParsed(category)) {
            value = ctx.getParsedObject(category, type);
        } else {
            value = ctx.getParsedDataGateway().readParsedObject(category, type);
        }
        if (value == null) {
            throw emptyParsed(category, sheetCode);
        }
        return value;
    }

    public static <E> E requireObjectPreloaded(LedgerBuildContext ctx,
                                               FileCategoryEnum category,
                                               Class<E> type,
                                               LedgerSheetCode sheetCode) {
        ensureFilePresent(ctx, category, sheetCode);
        if (!ctx.hasParsed(category)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: " + sheetCode.getDisplayName()
                    + "，缺少预加载解析结果，类别=" + categoryDisplayName(category));
        }
        E value = ctx.getParsedObject(category, type);
        if (value == null) {
            throw emptyParsed(category, sheetCode);
        }
        return value;
    }

    public static Workbook requireSourceWorkbook(LedgerBuildContext ctx,
                                                 FileCategoryEnum category,
                                                 LedgerSheetCode sheetCode) {
        ensureFilePresent(ctx, category, sheetCode);
        try {
            return ctx.getParsedDataGateway().openSourceWorkbook(category);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: " + sheetCode.getDisplayName()
                    + "，读取源文件失败，类别=" + categoryDisplayName(category) + "，原因=" + ex.getMessage());
        }
    }

    private static void ensureFilePresent(LedgerBuildContext ctx,
                                          FileCategoryEnum category,
                                          LedgerSheetCode sheetCode) {
        List<FileRecord> files = ctx.getFiles();
        boolean exists = files != null && files.stream().anyMatch(file -> file != null && file.getFileCategory() == category);
        if (!exists) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: " + sheetCode.getDisplayName()
                    + "，缺少输入文件类别=" + categoryDisplayName(category));
        }
    }

    private static BizException emptyParsed(FileCategoryEnum category, LedgerSheetCode sheetCode) {
        return new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: " + sheetCode.getDisplayName()
                + "，解析结果为空，类别=" + categoryDisplayName(category));
    }

    private static String categoryDisplayName(FileCategoryEnum category) {
        if (category == null) {
            return "";
        }
        if (StringUtils.hasText(category.getDisplayName())) {
            return category.getDisplayName();
        }
        return category.name();
    }
}
