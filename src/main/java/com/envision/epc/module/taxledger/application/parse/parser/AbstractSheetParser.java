package com.envision.epc.module.taxledger.application.parse.parser;

import com.envision.epc.module.taxledger.application.parse.EngineType;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseMeta;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.ParseSeverity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 解析模板最小骨架：
 * 1) 校验输入与读取字节
 * 2) 打开工作簿对象（引擎相关）
 * 3) 解析业务数据（子类实现）
 */
public abstract class AbstractSheetParser<T> implements SheetParser<T> {

    @Override
    public final ParseResult<T> parse(InputStream inputStream, ParseContext context) {
        long start = System.currentTimeMillis();
        ParseResult<T> result = ParseResult.<T>builder()
                .issues(new ArrayList<>())
                .build();

        if (inputStream == null) {
            result.addIssue(ParseSeverity.ERROR, "EMPTY_STREAM", "inputStream is null");
            result.setMeta(buildMeta(null, Collections.emptyList(), start));
            return result;
        }

        byte[] fileBytes = readAllBytes(inputStream, result);
        if (result.hasError()) {
            result.setMeta(buildMeta(null, Collections.emptyList(), start));
            return result;
        }

        Object workbook = openWorkbook(fileBytes, context, result);
        if (result.hasError() || workbook == null) {
            result.setMeta(buildMeta(null, Collections.emptyList(), start));
            return result;
        }

        ParsedPayload<T> payload = parseData(workbook, context, result);
        if (payload != null) {
            result.setData(payload.data());
            result.setMeta(buildMeta(payload.sheetName(), payload.headers(), start));
        } else {
            result.setMeta(buildMeta(null, Collections.emptyList(), start));
        }
        return result;
    }

    protected abstract Object openWorkbook(byte[] fileBytes, ParseContext context, ParseResult<T> result);

    protected abstract ParsedPayload<T> parseData(Object workbook, ParseContext context, ParseResult<T> result);

    protected abstract EngineType engineType();

    protected ParseMeta buildMeta(String sheetName, List<String> headers, long start) {
        return ParseMeta.builder()
                .engineType(engineType())
                .sheetName(sheetName)
                .headerSnapshot(headers == null ? Collections.emptyList() : headers)
                .templateVersion("v1")
                .elapsedMs(System.currentTimeMillis() - start)
                .build();
    }

    protected byte[] readAllBytes(InputStream inputStream, ParseResult<T> result) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length == 0) {
                result.addIssue(ParseSeverity.ERROR, "EMPTY_FILE", "inputStream has no bytes");
            }
            return bytes;
        } catch (IOException e) {
            result.addIssue(ParseSeverity.ERROR, "READ_STREAM_ERROR", e.getMessage());
            return new byte[0];
        }
    }

    public record ParsedPayload<T>(T data, String sheetName, List<String> headers) {
    }
}

