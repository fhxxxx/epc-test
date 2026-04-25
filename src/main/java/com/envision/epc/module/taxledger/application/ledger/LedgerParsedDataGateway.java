package com.envision.epc.module.taxledger.application.ledger;

import com.aspose.cells.Workbook;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.service.ParsedResultReader;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析结果读取门面（单次构建运行内缓存）
 */
public class LedgerParsedDataGateway {
    private final ParsedResultReader parsedResultReader;
    private final BlobStorageRemote blobStorageRemote;
    private final ObjectMapper objectMapper;
    private final Map<FileCategoryEnum, FileRecord> latestByCategory;
    private final Map<String, Object> parsedCache = new HashMap<>();
    private final Map<FileCategoryEnum, byte[]> sourceFileCache = new HashMap<>();

    public LedgerParsedDataGateway(ParsedResultReader parsedResultReader,
                                   BlobStorageRemote blobStorageRemote,
                                   ObjectMapper objectMapper,
                                   List<FileRecord> files) {
        this.parsedResultReader = parsedResultReader;
        this.blobStorageRemote = blobStorageRemote;
        this.objectMapper = objectMapper;
        this.latestByCategory = toLatestFileMap(files);
    }

    public <T> T readParsedObject(FileCategoryEnum category, Class<T> clazz) {
        String cacheKey = category.name() + "#OBJ#" + clazz.getName();
        Object cached = parsedCache.get(cacheKey);
        if (cached != null) {
            return clazz.cast(cached);
        }
        FileRecord file = requiredFile(category);
        T data = parsedResultReader.readParsedData(file.getParseResultBlobPath(), clazz);
        parsedCache.put(cacheKey, data);
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> readParsedList(FileCategoryEnum category, Class<T> elementClazz) {
        String cacheKey = category.name() + "#LIST#" + elementClazz.getName();
        Object cached = parsedCache.get(cacheKey);
        if (cached != null) {
            return (List<T>) cached;
        }
        FileRecord file = requiredFile(category);
        List<T> data = parsedResultReader.readParsedList(file.getParseResultBlobPath(), elementClazz);
        parsedCache.put(cacheKey, data);
        return data;
    }

    public Workbook openSourceWorkbook(FileCategoryEnum category) {
        try {
            byte[] content = sourceFileCache.computeIfAbsent(category, this::loadSourceBytes);
            return new Workbook(new ByteArrayInputStream(content));
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "打开源文件失败: " + category.getDisplayName() + " - " + e.getMessage());
        }
    }

    public JsonEnvelope readParsedEnvelope(FileCategoryEnum category) {
        try {
            FileRecord file = requiredFile(category);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(file.getParseResultBlobPath(), output);
            return objectMapper.readValue(output.toByteArray(), JsonEnvelope.class);
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "读取解析结果失败: " + category.getDisplayName() + " - " + e.getMessage());
        }
    }

    private byte[] loadSourceBytes(FileCategoryEnum category) {
        try {
            FileRecord file = requiredFile(category);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(file.getBlobPath(), output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "读取源文件失败: " + category.getDisplayName() + " - " + e.getMessage());
        }
    }

    private FileRecord requiredFile(FileCategoryEnum category) {
        FileRecord file = latestByCategory.get(category);
        if (file == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "缺少文件: " + category.getDisplayName());
        }
        return file;
    }

    private Map<FileCategoryEnum, FileRecord> toLatestFileMap(List<FileRecord> files) {
        Map<FileCategoryEnum, FileRecord> map = new HashMap<>();
        if (files == null || files.isEmpty()) {
            return map;
        }
        for (FileRecord file : files) {
            if (file == null || file.getFileCategory() == null) {
                continue;
            }
            FileRecord existing = map.get(file.getFileCategory());
            Long currentId = file.getId() == null ? 0L : file.getId();
            Long existingId = existing == null || existing.getId() == null ? 0L : existing.getId();
            if (existing == null || currentId > existingId) {
                map.put(file.getFileCategory(), file);
            }
        }
        return map;
    }

    public static class JsonEnvelope {
        public String schemaVersion;
        public Object result;
    }
}
