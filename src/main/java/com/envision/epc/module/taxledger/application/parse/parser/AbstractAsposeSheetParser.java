package com.envision.epc.module.taxledger.application.parse.parser;

import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.ParseSeverity;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAsposeSheetParser<T> extends AbstractSheetParser<T> {

    protected int headerRowIndex() {
        return 0;
    }

    protected int dataStartRowIndex() {
        return 1;
    }

    @Override
    protected Object openWorkbook(byte[] fileBytes, ParseContext context, ParseResult<T> result) {
        try {
            return new Workbook(new ByteArrayInputStream(fileBytes));
        } catch (Exception e) {
            result.addIssue(ParseSeverity.ERROR, "INVALID_WORKBOOK", e.getMessage());
            return null;
        }
    }

    @Override
    protected ParsedPayload<T> parseData(Object workbookObj, ParseContext context, ParseResult<T> result) {
        String sheetName = locateSheet(workbookObj, context, result);
        if (result.hasError()) {
            return new ParsedPayload<>(null, sheetName, List.of());
        }

        HeaderData headerData = readHeader(workbookObj, sheetName, context, result);
        if (result.hasError()) {
            return new ParsedPayload<>(null, sheetName, headerData == null ? List.of() : headerData.headers());
        }

        BodyData bodyData = readBody(workbookObj, sheetName, context, result);
        if (result.hasError()) {
            return new ParsedPayload<>(null, sheetName, headerData.headers());
        }

        T data = mapToDto(headerData, bodyData, context, result);
        validateBusinessRules(data, headerData, bodyData, context, result);
        return new ParsedPayload<>(data, sheetName, headerData.headers());
    }

    protected String locateSheet(Object workbookObj, ParseContext context, ParseResult<T> result) {
        Workbook workbook = (Workbook) workbookObj;
        String preferred = context.getFileName();
        if (category().getTargetSheetName() != null) {
            preferred = category().getTargetSheetName();
        }
        if (preferred != null && workbook.getWorksheets().get(preferred) != null) {
            return preferred;
        }
        if (workbook.getWorksheets().getCount() == 0) {
            result.addIssue(ParseSeverity.ERROR, "SHEET_NOT_FOUND", "workbook has no sheet");
            return null;
        }
        return workbook.getWorksheets().get(0).getName();
    }

    protected HeaderData readHeader(Object workbookObj, String sheetName, ParseContext context, ParseResult<T> result) {
        Worksheet worksheet = ((Workbook) workbookObj).getWorksheets().get(sheetName);
        if (worksheet == null) {
            result.addIssue(ParseSeverity.ERROR, "SHEET_NOT_FOUND", "target sheet not found: " + sheetName);
            return new HeaderData(List.of());
        }
        Cells cells = worksheet.getCells();
        int maxCol = cells.getMaxDataColumn();
        if (maxCol < 0) {
            result.addIssue(ParseSeverity.ERROR, "HEADER_NOT_FOUND", "header row is empty");
            return new HeaderData(List.of());
        }
        List<String> headers = new ArrayList<>();
        for (int col = 0; col <= maxCol; col++) {
            headers.add(cells.get(headerRowIndex(), col).getStringValue().trim());
        }
        return new HeaderData(headers);
    }

    protected BodyData readBody(Object workbookObj, String sheetName, ParseContext context, ParseResult<T> result) {
        Worksheet worksheet = ((Workbook) workbookObj).getWorksheets().get(sheetName);
        Cells cells = worksheet.getCells();
        int maxRow = cells.getMaxDataRow();
        int maxCol = cells.getMaxDataColumn();
        List<List<String>> rows = new ArrayList<>();
        for (int row = dataStartRowIndex(); row <= maxRow; row++) {
            List<String> values = new ArrayList<>();
            boolean hasValue = false;
            for (int col = 0; col <= maxCol; col++) {
                String cellValue = cells.get(row, col).getStringValue().trim();
                values.add(cellValue);
                if (!cellValue.isBlank()) {
                    hasValue = true;
                }
            }
            if (hasValue) {
                rows.add(values);
            }
        }
        return new BodyData(rows);
    }

    protected abstract T mapToDto(HeaderData headerData, BodyData bodyData, ParseContext context, ParseResult<T> result);

    protected void validateBusinessRules(T data,
                                         HeaderData headerData,
                                         BodyData bodyData,
                                         ParseContext context,
                                         ParseResult<T> result) {
    }

    protected String findCell(List<String> row, HeaderData headerData, String... aliases) {
        for (String alias : aliases) {
            int index = findHeaderIndex(headerData, alias);
            if (index >= 0 && index < row.size()) {
                return row.get(index);
            }
        }
        return "";
    }

    protected int findHeaderIndex(HeaderData headerData, String alias) {
        String normalized = normalize(alias);
        for (int i = 0; i < headerData.headers().size(); i++) {
            String header = normalize(headerData.headers().get(i));
            if (header.equals(normalized) || header.contains(normalized)) {
                return i;
            }
        }
        return -1;
    }

    protected String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .replace("\t", "")
                .replace("（", "(")
                .replace("）", ")")
                .trim();
    }

    public record HeaderData(List<String> headers) {
    }

    public record BodyData(List<List<String>> rows) {
    }
}

