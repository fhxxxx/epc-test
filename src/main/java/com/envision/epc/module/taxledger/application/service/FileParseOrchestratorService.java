package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SheetParseService;
import com.envision.epc.module.taxledger.domain.FileParseStatusEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileParseOrchestratorService {
    private static final int MAX_ERR_LEN = 1000;
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String RESULT_SCHEMA_VERSION = "v1";

    private final FileRecordMapper fileRecordMapper;
    private final BlobStorageRemote blobStorageRemote;
    private final SheetParseService sheetParseService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;

    public void parseAsync(Long fileRecordId, String operator) {
        if (fileRecordId == null) {
            return;
        }
        taskExecutor.execute(() -> parseAsyncInternal(fileRecordId, operator));
    }

    public String loadParsedResultOrParse(FileRecord record, String operator) {
        if (record == null || record.getId() == null || record.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "File not found");
        }
        if (record.getParseStatus() == FileParseStatusEnum.SUCCESS
                && StringUtils.hasText(record.getParseResultBlobPath())) {
            return loadBlobAsString(record.getParseResultBlobPath());
        }

        return parseNow(record, operator);
    }

    public String parseNow(FileRecord record, String operator) {
        if (record == null || record.getId() == null || record.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "File not found");
        }

        int claimed = fileRecordMapper.transitionParseStatus(
                record.getId(),
                FileParseStatusEnum.PARSING,
                new FileParseStatusEnum[]{FileParseStatusEnum.PENDING, FileParseStatusEnum.FAILED}
        );
        if (claimed <= 0) {
            FileRecord latest = getActiveById(record.getId());
            if (latest != null
                    && latest.getParseStatus() == FileParseStatusEnum.SUCCESS
                    && StringUtils.hasText(latest.getParseResultBlobPath())) {
                return loadBlobAsString(latest.getParseResultBlobPath());
            }
            ParseExecutionResult direct = executeParse(record, operator);
            if (direct.hasError) {
                throw new BizException(ErrorCode.BAD_REQUEST, truncate(direct.errorMessage));
            }
            return direct.jsonPayload;
        }

        ParseExecutionResult parsed = executeParse(record, operator);
        if (parsed.hasError) {
            fileRecordMapper.markParseFailed(
                    record.getId(),
                    FileParseStatusEnum.FAILED,
                    FileParseStatusEnum.PARSING,
                    truncate(parsed.errorMessage)
            );
            throw new BizException(ErrorCode.BAD_REQUEST, truncate(parsed.errorMessage));
        }

        String path = uploadResultJson(record, parsed.jsonPayload);
        fileRecordMapper.markParseSuccess(
                record.getId(),
                FileParseStatusEnum.SUCCESS,
                FileParseStatusEnum.PARSING,
                path,
                LocalDateTime.now()
        );
        return parsed.jsonPayload;
    }

    public void deleteParseResultIfExists(FileRecord record) {
        if (record == null || !StringUtils.hasText(record.getParseResultBlobPath())) {
            return;
        }
        try {
            blobStorageRemote.delete(record.getParseResultBlobPath());
        } catch (Exception e) {
            log.warn("delete parse result blob failed, fileId={}, path={}",
                    record.getId(), record.getParseResultBlobPath(), e);
        }
    }

    private void parseAsyncInternal(Long fileRecordId, String operator) {
        int claimed = fileRecordMapper.transitionParseStatus(
                fileRecordId,
                FileParseStatusEnum.PARSING,
                new FileParseStatusEnum[]{FileParseStatusEnum.PENDING, FileParseStatusEnum.FAILED}
        );
        if (claimed <= 0) {
            return;
        }

        FileRecord record = getActiveById(fileRecordId);
        if (record == null) {
            return;
        }

        try {
            ParseExecutionResult parsed = executeParse(record, operator);
            if (parsed.hasError) {
                fileRecordMapper.markParseFailed(
                        record.getId(),
                        FileParseStatusEnum.FAILED,
                        FileParseStatusEnum.PARSING,
                        truncate(parsed.errorMessage)
                );
                return;
            }

            String path = uploadResultJson(record, parsed.jsonPayload);
            fileRecordMapper.markParseSuccess(
                    record.getId(),
                    FileParseStatusEnum.SUCCESS,
                    FileParseStatusEnum.PARSING,
                    path,
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            fileRecordMapper.markParseFailed(
                    record.getId(),
                    FileParseStatusEnum.FAILED,
                    FileParseStatusEnum.PARSING,
                    truncate(e.getMessage())
            );
            log.error("file parse async failed, fileId={}", record.getId(), e);
        }
    }

    private ParseExecutionResult executeParse(FileRecord record, String operator) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        try (InputStream inputStream = loadBlobAsInputStream(record.getBlobPath())) {
            ParseContext context = ParseContext.builder()
                    .companyCode(record.getCompanyCode())
                    .yearMonth(record.getYearMonth())
                    .fileName(record.getFileName())
                    .operator(StringUtils.hasText(operator) ? operator : "system")
                    .traceId(traceId)
                    .build();
            ParseResult<?> parseResult = sheetParseService.parse(inputStream, record.getFileCategory(), context);
            String payload = toJsonPayload(record, parseResult, traceId);

            if (parseResult == null || parseResult.hasError()) {
                String msg = parseResult == null ? "parse result is null" : objectMapper.writeValueAsString(parseResult.getIssues());
                return ParseExecutionResult.failed(payload, msg);
            }
            return ParseExecutionResult.success(payload);
        } catch (Exception e) {
            return ParseExecutionResult.failed(null, e.getMessage());
        }
    }

    private String toJsonPayload(FileRecord record, ParseResult<?> parseResult, String traceId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", RESULT_SCHEMA_VERSION);
        payload.put("sourceFileId", record.getId());
        payload.put("companyCode", record.getCompanyCode());
        payload.put("yearMonth", record.getYearMonth());
        payload.put("fileCategory", record.getFileCategory() == null ? null : record.getFileCategory().name());
        payload.put("traceId", traceId);
        payload.put("parsedAt", LocalDateTime.now());
        payload.put("result", parseResult);
        return objectMapper.writeValueAsString(payload);
    }

    private String uploadResultJson(FileRecord record, String payload) {
        String path = String.format("tax-ledger/%s/%s/%s/parsed/%d_%s.json",
                record.getCompanyCode(),
                record.getYearMonth(),
                record.getFileCategory().name(),
                record.getId(),
                LocalDateTime.now().format(TS_FORMATTER));
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        blobStorageRemote.upload(path, new ByteArrayInputStream(bytes));
        return path;
    }

    private FileRecord getActiveById(Long id) {
        return fileRecordMapper.selectOne(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getId, id)
                .eq(FileRecord::getIsDeleted, 0));
    }

    private InputStream loadBlobAsInputStream(String path) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        blobStorageRemote.loadStream(path, output);
        return new ByteArrayInputStream(output.toByteArray());
    }

    private String loadBlobAsString(String path) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        blobStorageRemote.loadStream(path, output);
        return output.toString(StandardCharsets.UTF_8);
    }

    private static String truncate(String msg) {
        if (!StringUtils.hasText(msg)) {
            return "parse failed";
        }
        return msg.length() <= MAX_ERR_LEN ? msg : msg.substring(0, MAX_ERR_LEN);
    }

    private static class ParseExecutionResult {
        private final String jsonPayload;
        private final boolean hasError;
        private final String errorMessage;

        private ParseExecutionResult(String jsonPayload, boolean hasError, String errorMessage) {
            this.jsonPayload = jsonPayload;
            this.hasError = hasError;
            this.errorMessage = errorMessage;
        }

        static ParseExecutionResult success(String payload) {
            return new ParseExecutionResult(payload, false, null);
        }

        static ParseExecutionResult failed(String payload, String errorMessage) {
            return new ParseExecutionResult(payload, true, errorMessage);
        }
    }
}

