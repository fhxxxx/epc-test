package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.ParsedResultTypeCatalog;
import com.envision.epc.module.taxledger.application.ledger.data.BusinessSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;

import java.util.List;

/**
 * 真实业务 Sheet Builder 基类（预加载优先，网关回退）
 */
public abstract class AbstractParsedBusinessSheetDataBuilder<T extends BusinessSheetData.ParsedSheetData<?>>
        implements LedgerSheetDataBuilder<T> {

    protected abstract LedgerSheetCode code();

    @Override
    public LedgerSheetCode support() {
        return code();
    }

    protected <E> List<E> readList(LedgerBuildContext ctx, FileCategoryEnum category, Class<E> elementType) {
        if (ctx.hasParsed(category)) {
            return ctx.getParsedList(category, elementType);
        }
        if (!hasCategoryFile(ctx, category)) {
            return List.of();
        }
        return ctx.getParsedDataGateway().readParsedList(category, elementType);
    }

    protected <E> E readObject(LedgerBuildContext ctx, FileCategoryEnum category, Class<E> type) {
        if (ctx.hasParsed(category)) {
            return ctx.getParsedObject(category, type);
        }
        if (!hasCategoryFile(ctx, category)) {
            return null;
        }
        return ctx.getParsedDataGateway().readParsedObject(category, type);
    }

    protected Object readByCatalog(LedgerBuildContext ctx, FileCategoryEnum category) {
        ParsedResultTypeCatalog.Entry entry = ParsedResultTypeCatalog.get(category);
        if (entry == null) {
            return null;
        }
        if (entry.shape() == ParsedResultTypeCatalog.Shape.LIST) {
            return readList(ctx, category, (Class<Object>) entry.valueType());
        }
        return readObject(ctx, category, (Class<Object>) entry.valueType());
    }

    private boolean hasCategoryFile(LedgerBuildContext ctx, FileCategoryEnum category) {
        List<FileRecord> files = ctx.getFiles();
        if (files == null || files.isEmpty()) {
            return false;
        }
        return files.stream().anyMatch(file -> file != null && file.getFileCategory() == category);
    }
}
